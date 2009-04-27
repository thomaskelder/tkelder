package pps.pathwayexport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataDerby;
import org.bridgedb.DataException;
import org.bridgedb.DataSource;
import org.bridgedb.SimpleGdb;
import org.bridgedb.SimpleGdbFactory;
import org.bridgedb.Xref;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.util.FileUtils;

import pps.interaction.GridLayout;

public class Genelist2Pathway {
	private static final int V_OFFSET = 20*15;
	private static final int H_OFFSET = 10*15;
	private static final int WIDTH = 10;
	
	@Option(name = "-in", required = true, usage = "The file or path containing the gene set text files to export.")
	private File in;

	@Option(name = "-out", required = true, usage = "The output path to write the GPML files to.")
	private File out;

	@Option(name = "-code", required = true, usage = "The system code of the genes in the sets.")
	private String sysCode;

	@Option(name = "-gdb", required = false, usage = "A synonym database to get symbols from.")
	private File gdbFile;
	
	@Option(name = "-toCode", required = false, usage = "Translate the genes to the given database system.")
	private String toSysCode;
	
	public static void main(String[] args) {
		PreferenceManager.init();
		Logger.log.setLogLevel(true, true, true, true, true, true);

		Genelist2Pathway main = new Genelist2Pathway();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		try {
			SimpleGdb gdb = null;
			if(main.gdbFile != null) {
				gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			}
			
			DataSource ds = DataSource.getBySystemCode(main.sysCode);
			
			DataSource toDs = null;
			if(main.toSysCode != null) {
				toDs = DataSource.getBySystemCode(main.toSysCode);
			}
			
			GridLayout layout = new GridLayout(WIDTH);

			for(File f : FileUtils.getFiles(main.in, true)) {
				Set<Xref> xrefs = readXrefs(f, ds);
				if(toDs != null) {
					Set<Xref> dsXrefs = new HashSet<Xref>();
					for(Xref x : xrefs) dsXrefs.addAll(gdb.getCrossRefs(x, toDs));
					xrefs = dsXrefs;
				}
				
				if(xrefs.size() > 0) {
					Pathway p = new Pathway();
					p.getMappInfo().setMapInfoName(f.getName());
					
					//Add a datanode containing the file name
					PathwayElement mainElm = PathwayElement.createPathwayElement(ObjectType.DATANODE);
					mainElm.setInitialSize();
					mainElm.setMLeft(H_OFFSET);
					mainElm.setMTop(V_OFFSET);
					mainElm.setTextLabel(getSymbol(new Xref(f.getName(), ds), gdb));
					mainElm.setGeneID(f.getName());
					mainElm.setDataSource(ds);
					p.add(mainElm);
					
					Set<PathwayElement> datanodes = new HashSet<PathwayElement>();
					
					for(Xref x : xrefs) {
						PathwayElement pwe = PathwayElement.createPathwayElement(ObjectType.DATANODE);
						pwe.setInitialSize();
						pwe.setGeneID(x.getId());
						pwe.setDataSource(x.getDataSource());
						
						pwe.setTextLabel(getSymbol(x, gdb));
						
						p.add(pwe);
						datanodes.add(pwe);
					}
					layout.layout(datanodes, H_OFFSET, 2 * V_OFFSET + mainElm.getMHeight());
					File gpmlFile = new File(main.out, f.getName() + ".gpml");
					File svgFile = new File(main.out, f.getName() + ".svg");
					p.writeToXml(gpmlFile, true);
					p.writeToSvg(svgFile);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static String getSymbol(Xref x, SimpleGdb gdb) throws DataException {
		String symbol = x.getId();
		if(gdb != null) {
			String gs = gdb.getGeneSymbol(x);
			if(gs != null) symbol = gs;
			else { //Try to get via ensembl
				List<Xref> cross = gdb.getCrossRefs(x, DataSource.ENSEMBL);
				if(cross != null && cross.size() > 0) {
					gs = gdb.getGeneSymbol(cross.get(0));
					if(gs != null) symbol = gs;
				}
			}
		}
		return symbol;
	}
	
	private static Set<Xref> readXrefs(File setFile, DataSource ds) throws IOException {
		Set<Xref> xrefs = new HashSet<Xref>();

		BufferedReader in = new BufferedReader(new FileReader(setFile));
		String line = null;
		while((line = in.readLine()) != null) {
			line.trim();
			xrefs.add(new Xref(line.trim(), ds));
		}

		return xrefs;
	}
}
