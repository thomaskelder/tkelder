package walkietalkie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Create GPML files containing protein-pathway associations from Reactome.
 * @author thomas
 */
public class ReactomeSets {
	@Option(name = "-in", required = true, usage = "The reactome curated_and_inferred_uniprot_2_pathways.txt file.")
	private File in;
	@Option(name = "-out", required = true, usage = "The directory to write the GPML files to.")
	private File out;
	@Option(name = "-species", required = false, usage = "The species to extract the pathway sets for.")
	private String species;
	
	public static void main(String[] args) {
		ReactomeSets main = new ReactomeSets();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			writeReactomeSets(main.in, main.out, main.species);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
	
	//P26439	UniProt:P26439	[2 processes]: Hormone biosynthesis; Metabolism of lipids and lipoproteins	http://www.reactome.org/cgi-bin/link?SOURCE=UniProt&ID=P26439	Homo sapiens
	
	public static void writeReactomeSets(File reactFile, File outDir, String species) throws IOException, ConverterException {
		BufferedReader in = new BufferedReader(new FileReader(reactFile));

		Multimap<String, Xref> pathway2xref = new HashMultimap<String, Xref>();
		
		String line = null;
		List<String[]> rows = new ArrayList<String[]>();
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t");
			String org = cols[4];
			if(species == null || species.equals(org)) {
				Xref x = new Xref(cols[0], BioDataSource.UNIPROT);
				String procLine = cols[2];
				procLine = procLine.replaceAll("\\[.+\\]: ", "");
				String[] pws = procLine.split("; ");
				for(String p : pws) pathway2xref.put(p, x);
			}
		}
		
		in.close();
		
		for(String pn : pathway2xref.keySet()) {
			Pathway p = new Pathway();
			p.getMappInfo().setOrganism(species);
			p.getMappInfo().setMapInfoName(pn);
			for(Xref x : pathway2xref.get(pn)) {
				PathwayElement px = PathwayElement.createPathwayElement(ObjectType.DATANODE);
				px.setInitialSize();
				px.setDataSource(x.getDataSource());
				px.setGeneID(x.getId());
				p.add(px);
			}
			File pf = new File(outDir, pn.replaceAll("[\\/:\"*?<>|]+", "") + ".gpml");
			p.writeToXml(pf, true);
		}
	}
}
