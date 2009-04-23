package org.bridgedb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;

/**
 * Create a simple synonym database from a biomart file, containing three columns:
 * Ensembl gene id	\t	Ensembl protein id	\t	Gene symbol.
 * 
 * @author thomas
 */
//TODO: Don't hardcode datasources (ENSG, ENSP), but allow to specify this as input parameter
public class EnsemblGeneToProtein {
	public static final String ENSP_CODE = "EP";
	
	@Option(name = "-in", required = true, usage = "The biomart file, with one header line and " +
			" three tab-separated columns:" +
			"\n-gene id\n-protein id\n-symbol (optional)")
	private File martFile;
	
	@Option(name = "-gdb", required = true, usage = "The gdb file to create.")
	private File gdbFile;
	
	public static void main(String[] args) {
		EnsemblGeneToProtein main = new EnsemblGeneToProtein();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			createGdb(main.martFile, main.gdbFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void registerEnspDataSource() {
		DataSource.register("EP", "Ensembl Protein", null, null, null, false, false, null);
	}
	
	/**
	 * Martfile format:
	 * Gene ID \t Protein ID \t Gene name
	 * @param martFile
	 * @param gdbFile
	 * @throws DataException
	 * @throws IOException 
	 */
	public static void createGdb(File martFile, File gdbFile) throws DataException, IOException {
		Logger.log.setLogLevel(true, true, true, true, true, true);
		registerEnspDataSource();
		
		SimpleGdb gdb = SimpleGdbFactory.createInstance(
				"" + gdbFile,
				new DataDerby(), 
				DBConnector.PROP_RECREATE
		);
		
		gdb.createGdbTables();
		gdb.preInsert();
		
		BufferedReader in = new BufferedReader(new FileReader(martFile));
		String line = in.readLine(); //Skip the first line
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t",3);
			if(cols[0] == null || cols[1] == null ||
					"".equals(cols[0]) || "".equals(cols[1])) {
				continue;
			}
			
			Xref gene = new Xref(cols[0], DataSource.ENSEMBL);
			Xref protein = new Xref(cols[1], DataSource.getBySystemCode(ENSP_CODE));
			
			Logger.log.trace("Adding " + gene + ", " + protein);
			gdb.addGene(gene, "");
			gdb.addGene(protein, "");
			
			gdb.addLink(gene, gene);
			gdb.addLink(gene, protein);
			
			if(cols[2] != null && !"".equals(cols[2])) { //A symbol is present
				gdb.addAttribute(gene, "Symbol", cols[2]);
				gdb.addAttribute(protein, "Symbol", cols[2]);
			}
		}
		
		in.close();
		
		gdb.createGdbIndices();
		gdb.compact();
		gdb.finalize();
		gdb.close();
	}
}
