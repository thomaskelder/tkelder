package pps.pathwayexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataDerby;
import org.bridgedb.DataSource;
import org.bridgedb.Gdb;
import org.bridgedb.SimpleGdbFactory;
import org.bridgedb.Xref;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.indexer.RelationshipIndexer;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.util.FileUtils;

/**
 * Creates a single sif file from a set of GPML pathways, by parsing
 * the interactions defined in GPML.
 * @author thomas
 */
public class SifExporter {
	@Option(name = "-in", required = true, usage = "The gpml file or path containing the gpmls to export (" +
			"in the latter case, the sif will contain the merged gpmls).")
	private File in;
	
	@Option(name = "-out", required = true, usage = "The sif file to write to.")
	private File out;
	
	@Option(name = "-code", required = true, usage = "The code of the DataSource to translate to")
	private String sysCode;
	
	@Option(name = "-gdb", required = true, usage = "The synonym database to use.")
	private File gdbFile;
	
	/**
	 * Parses all interactions in a set of GPML pathways and
	 * generates pathways centered around all interactions for
	 * a single DataNode.
	 */
	private void exportNodeGpml() {
		
	}
	
	public static void main(String[] args) {
		Logger.log.setLogLevel(true, true, true, true, true, true);

		SifExporter main = new SifExporter();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		try {
			Gdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			DataSource ds = DataSource.getBySystemCode(main.sysCode);
			if(ds == null) {
				Logger.log.error("Couldn't find database system for code '" + main.sysCode + "'");
				System.exit(-1);
			}
			
			List<File> files = null;
			if(main.in.isDirectory()) {
				files = FileUtils.getFiles(main.in, "gpml", true);
			} else {
				files = new ArrayList<File>();
				files.add(main.in);
			}
			if(files.size() == 0) {
				Logger.log.error("No gpml files found in " + main.in);
			}
			
			Set<Interaction> interactions = new HashSet<Interaction>();
			
			for(File f : files) {
				Logger.log.trace("Collecting interactions for " + f.getName());

				Pathway pathway = new Pathway();
				pathway.readFromXml(f, true);
				
				for(PathwayElement pwe : pathway.getDataObjects()) {
					if(RelationshipIndexer.isRelation(pwe)) {
						Relation r = new Relation(pwe);
						for(PathwayElement left : r.getLefts()) {
							if(!checkXref(left.getXref())) continue;
							for(Xref lx : gdb.getCrossRefs(left.getXref(), ds)) {
								for(PathwayElement right : r.getRights()) {
									if(!checkXref(right.getXref())) continue;
									for(Xref rx : gdb.getCrossRefs(right.getXref(), ds)) {
										interactions.add(
												toInteraction(left, lx, right, rx, "unkown")
										);
									}
								}
							}
						}
						for(PathwayElement mediator : r.getMediators()) {
							if(!checkXref(mediator.getXref())) continue;
							for(Xref mx : gdb.getCrossRefs(mediator.getXref(), ds)) {
								for(PathwayElement right : r.getRights()) {
									if(!checkXref(right.getXref())) continue;
									for(Xref rx : gdb.getCrossRefs(right.getXref(), ds)) {
										interactions.add(
												toInteraction(mediator, mx, right, rx, "mediator")
										);
									}
								}
								for(PathwayElement left : r.getLefts()) {
									if(!checkXref(left.getXref())) continue;
									for(Xref lx : gdb.getCrossRefs(left.getXref(), ds)) {
										interactions.add(
												toInteraction(mediator, mx, left, lx, "mediator")
										);
									}
								}
							}
						}
					}
				}
			}

			Writer out = new BufferedWriter(new FileWriter(main.out));

			for(Interaction i : interactions) {
				out.append(i.id1);
				out.append("\t");
				out.append(i.type);
				out.append("\t");
				out.append(i.id2);
				out.append("\n");
			}

			out.flush();
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Interaction toInteraction(PathwayElement p1, Xref x1, PathwayElement p2, Xref x2, String type) {
		String id1 = x1.getId();
		String id2 = x2.getId();
		return new Interaction(id1, id2, type);
	}
	
	private static boolean checkXref(Xref r) {
		if(r == null) return false;
		if(r.getDataSource() == null) return false;
		if(r.getId() == null) return false;
		return true;
	}
	
	private static class Interaction {
		String id1;
		String id2;
		String type;
		public Interaction(String id1, String id2, String type) {
			this.id1 = id1;
			this.id2 = id2;
			this.type = type;
		}

		public int hashCode() {
			return (id1 + id2 + type).hashCode();
		}
		
		public boolean equals(Object o) {
			return (id1 + id2 + type).equals(o);
		}
	}
}
