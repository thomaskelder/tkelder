package org.pct.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bridgedb.Xref;
import org.bridgedb.bio.Organism;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.Utils;
import org.pct.conversion.PsiMiImport;
import org.pct.model.AttributeKey;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;

import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Convert PSI-MI-Tab files from various datasources to a JUNG
 * network and print some statistics.
 */
public class RunPsiMiTabConversion {
	private final static Logger log = Logger.getLogger(RunPsiMiTabConversion.class.getName());

	static final String psiRegistryUrl =
		"http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=ACTIVE&format=txt";
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			Organism organism = Organism.fromLatinName(pargs.getSpecies());
			if(organism == null) {
				log.severe("Can't find species for '" + pargs.getSpecies() + 
						"', please use latin name, e.g. 'Homo sapiens'");
				return;
			}
			
			Set<File> psiMiFiles = new HashSet<File>();
			Set<File> xgmmlFiles = new HashSet<File>();
			
			//Find out which services are available and download data
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new URL(psiRegistryUrl).openStream()));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("=");
				String name = parts[0];
				String url = parts[1];
				
				if(pargs.isIgnore()) {
					boolean ignore = false;
					for(String i : pargs.getIgnore()) {
						if(name.matches(i)) {
							log.info("Ignoring " + name + ", matches pattern " + i);
							ignore = true;
							break;
						}
					}
					if(ignore) continue;
				}
				
				if(url.endsWith("psicquic")) url = url.substring(0, url.length() - 8);
				url += "current/search/query/species:" + PsiMiImport.taxIdFromSpecies.get(organism);
				
				String countUrl = url + "?format=count";
				int count = Integer.parseInt(
						new BufferedReader(new InputStreamReader(new URL(countUrl).openStream())).readLine()
				);
				
				int part = 1000;
				for(int i = 0; i < count / part + 1; i++) {
					int start = i * part;
					File cf = new File(pargs.getCache(), name + "_" + organism.latinName() + "_" + start + ".txt");
					if(!cf.exists()) {
						String partUrl = url + "?firstResult=" + start + "&maxResults=" + part;
						log.info("Downloading " + name + " (" + partUrl + ")");
						FileUtils.downloadFile(new URL(partUrl), cf);
					} else {
						log.info("Using cached file " + cf);
					}
					psiMiFiles.add(cf);
				}
			}

			in.close();
			
			//Convert to network
			String report = "";

			//Each file separate
			PsiMiImport pin = new PsiMiImport();
			pin.setFilterSpecies(organism);
			pin.setIdmapper(didm.getIDMapper());
			pin.setCommonDs(didm.getDataSources());
			pin.setIgnoreInteractions(pargs.getIgnoreInteractions());
			
			for(File f : psiMiFiles) {
				File outFile = new File(pargs.getOut(), f.getName().replace(".txt", ".xgmml"));
				xgmmlFiles.add(outFile);
				
				if(!outFile.exists() || pargs.isOverwrite()) {
					report += "-> " + f.getName() + "\n";
		
					Network<Xref, String> n = pin.createNetwork(Utils.setOf(f));
					report += n.getNetworkAttribute(AttributeKey.ConversionReport.name()) + "\n";
		
					FileWriter out = new FileWriter(outFile);
					n.writeToXGMML(out);
					out.close();
				} else {
					log.info(outFile + " already exists, use --overwrite to force it to be converted again.");
				}
			}
			//All files in one network
			Network<Xref, String> network = new Network<Xref, String>(new JungGraph<Xref, String>());
			for(File f : xgmmlFiles) {
				Network<Xref, String> n = new Network<Xref, String>(new JungGraph<Xref, String>());
				n.readFromXGMML(new FileReader(f), Network.xrefFactory, Network.defaultFactory);
				network.merge(n);
			}

			FileWriter fout = new FileWriter(new File(
							pargs.getOut(),"allresources_" + organism.latinName() + ".xgmml"));
			network.writeToXGMML(fout);
			fout.close();

			PrintWriter out = new PrintWriter(new File(pargs.getOut(), "report.txt"));
			out.write(report);
			out.close();
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
			e.printStackTrace();
		}
	}
	
	interface Args extends AIDMapper, AHelp {
		@Option(shortName = "c", description = "The cache path to store the PSI-MI-Tab files")
		File getCache();
		@Option(shortName = "o", description = "The directory to write the xgmml and report files to")
		File getOut();
		@Option(description = "The species to filter for.")
		String getSpecies();
		@Option(description = "Overwrite already converted files.")
		boolean isOverwrite();
		@Option(defaultValue = { "colocalization" }, description = "Do not import interactions of given type.")
		List<String> getIgnoreInteractions();
		@Option(description = "Ignore resources that match this pattern.")
		List<String> getIgnore();
		boolean isIgnore();
	}
}
