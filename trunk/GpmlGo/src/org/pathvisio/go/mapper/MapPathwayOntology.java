package org.pathvisio.go.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.bio.BioDataSource;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;

/**
 * Map GO terms to Pathway Ontology terms
 * @author thomas
 *
 */
public class MapPathwayOntology {
	@Option(name = "-po", required = true, usage = "The Pathway Ontology tree file (obo flat format)")
	private File oboFilePO;

	@Option(name = "-go", required = true, usage = "The Gene Ontology tree file (obo flat format)")
	private File oboFileGO;
	
	@Option(name = "-in", usage = "The input file, containing GO ids")
	private File inFile;
	
	@Option(name = "-col", usage = "The column in which the GO ids can be found")
	private int col = 0;
	
	@Option(name = "-out", usage = "The file to which the output will be written")
	private File outFile;
	

	public static void main(String[] args) {
		BioDataSource.init();
		MapPathwayOntology main = new MapPathwayOntology();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			GOTree poTree = GOReader.readGOTree(main.oboFilePO);
			GOTree goTree = GOReader.readGOTree(main.oboFileGO);
			
			Pattern pat = Pattern.compile("GO\\\\:[0-9]+");
		    
			Map<String, GOTerm> go2po = new HashMap<String, GOTerm>();
			for(GOTerm po : poTree.getTerms()) {
				if(po.getDef() != null) {
					Matcher mat = pat.matcher(po.getDef());
					while(mat.find()) {
						String go = mat.group().replace("\\", "");
						go2po.put(go, po);
						
						//Also put child GO terms (if not annotated with specific po yet)
						for(GOTerm child : goTree.getChildren(go)) {
							if(!go2po.containsKey(child)) go2po.put(go, child);
						}
					}
				}
			}
			
			PrintWriter out = new PrintWriter(main.outFile);
			BufferedReader in = new BufferedReader(new FileReader(main.inFile));
			String line = null;
			while((line = in.readLine()) != null) {
				String[] cols = line.split("\t");
				String go = cols[main.col];
				GOTerm po = go2po.get(go);
				String poId = "";
				String poName = "";
				if(po != null) {
					poId = po.getId();
					poName = po.getName();
				}
				out.println(line + "\t" + poId + "\t" + poName);
			}
			
			out.close();
			in.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
}
