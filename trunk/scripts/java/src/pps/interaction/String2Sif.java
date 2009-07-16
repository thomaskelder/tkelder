package pps.interaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.bridgedb.DataSource;
import org.bridgedb.EnsemblGeneToProtein;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Convert a text file containing protein interactions downloaded from the
 * STRING database (http://string.embl.de) into a SIF file. Ensembl protein
 * ids will be converted into Ensembl gene ids, if a synonym database is provided.
 * @author thomas
 */
public class String2Sif {
	@Option(name = "-in", required = true, usage = "The protein interactions txt file " +
			"from STRING. This file contains three columns:\n" +
			"- An Ensembl protein id (prefixed with species number and dot).\n" +
			"- An Ensembl protein id (prefixed with species number and dot).\n" +
			"- An interaction score.")
	private File stringFile;
	
	@Option(name = "-out", required = true, usage = "The sif file to write to.")
	private File outFile;
	
	@Option(name = "-gdb", required = true, usage = "The synonym database to convert ENSP into ENSG.")
	private File gdbFile;
	
	@Option(name = "-attrFile", required = false, usage = "Optionally write symbol attributes to this file.")
	private File symbolAttrFile;
	
	public static void main(String[] args) {
		Logger.log.setLogLevel(true, true, true, true, true, true);
		EnsemblGeneToProtein.registerEnspDataSource();
		
		String2Sif main = new String2Sif();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			SimpleGdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			
			BufferedReader in = new BufferedReader(new FileReader(main.stringFile));
			Network network = fromStringDb(in, gdb);
			in.close();
			
			BufferedWriter out = new BufferedWriter(new FileWriter(main.outFile));
			network.toSif(out);
			out.flush();
			out.close();
			
			//Write symbol attributes
			if(main.symbolAttrFile != null) {
				Logger.log.trace("Writing attributes");
				BufferedWriter attrOut = new BufferedWriter(new FileWriter(main.symbolAttrFile));
				network.writeSymbols(attrOut);
				attrOut.flush();
				attrOut.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Import from a STRING database txt file
	 * @throws IOException 
	 * @throws IDMapperException 
	 * @throws DataException 
	 */
	public static Network fromStringDb(BufferedReader in, SimpleGdb gdb) throws IOException, IDMapperException {
		Network network = new Network();

		Multimap<Xref, Xref> xrefCache = new HashMultimap<Xref, Xref>();
		
		DataSource enspDs = DataSource.getBySystemCode(EnsemblGeneToProtein.ENSP_CODE);
		String line = null;
		int i = 0;
		while((line = in.readLine()) != null) {
			if(i % 100 == 0) {
				Logger.log.trace("Processing line " + i);
			}
			i++;
			//Node1 Node2 score
			String[] cols = line.split(" ",3);
			String id1 = removeSpeciesCode(cols[0]);
			String id2 = removeSpeciesCode(cols[1]);
			if(gdb != null) {
				Xref idx1 = new Xref(id1, enspDs);
				Xref idx2 = new Xref(id2, enspDs);
				for(Xref x1 : getCachedXrefs(idx1, xrefCache, gdb, BioDataSource.ENTREZ_GENE)) {
					for(Xref x2 : getCachedXrefs(idx2, xrefCache, gdb, BioDataSource.ENTREZ_GENE)) {
						Node n1 = Node.getInstance(x1.getId());
						Node n2 = Node.getInstance(x2.getId());
						network.addEdge(
							new Edge(
									n1, 
									n2, 
									cols[2]
							)	
						);
					}
				}
			} else {
				network.addEdge(
						new Edge(Node.getInstance(id1), Node.getInstance(id2), cols[2])	
				);
			}
		}
		
		i = 0;
		int size = network.getNodes().size();
		Logger.log.trace("Network created: " + size + " nodes.");
		for(Node n : network.getNodes()) {
			Logger.log.trace("Setting gene symbol for " + i++ + " / " + size);
			network.setSymbol(n, gdb.getGeneSymbol(new Xref(n.getId(), BioDataSource.ENTREZ_GENE)));
		}
		return network;
	}
	
	private static Collection<Xref> getCachedXrefs(Xref x, Multimap<Xref, Xref> cache, IDMapperRdb gdb, DataSource ds) throws IDMapperException {
		Collection<Xref> xcache = cache.get(x);
		if(xcache.size() == 0) {
			cache.putAll(x, xcache = gdb.getCrossRefs(x, ds));
		}
		return xcache;
	}
	
	private static String removeSpeciesCode(String id) {
		return id.replaceFirst("[0-9]+\\.", "");
	}
}
