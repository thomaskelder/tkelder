package org.pct.conversion;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bridgedb.AttributeMapper;
import org.bridgedb.DataSource;
import org.bridgedb.DataSourcePatterns;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.Utils;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

import psidev.psi.mi.tab.PsimiTabReader;
import psidev.psi.mi.tab.model.Author;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.CrossReference;
import psidev.psi.mi.tab.model.InteractionType;
import psidev.psi.mi.tab.model.Interactor;
import psidev.psi.mi.tab.model.builder.AbstractDocumentDefinition;
import psidev.psi.mi.tab.model.builder.DocumentDefinition;
import psidev.psi.mi.tab.model.builder.InteractionRowConverter;
import psidev.psi.mi.tab.model.builder.MitabInteractionRowConverter;
import psidev.psi.mi.tab.model.builder.RowBuilder;
import psidev.psi.mi.xml.converter.ConverterException;
import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Imports a PSI-MI-Tab (from sif/edge/node attribute files
 * to the JUNG model.
 * 
 * Get PSI-MI-Tab files for many PPI databases from:
 * http://www.ebi.ac.uk/Tools/webservices/psicquic/view/main.xhtml
 * 
 * @author thomas
 */
public class PsiMiImport {
	private final static Logger log = Logger.getLogger(PsiMiImport.class.getName());

	private static Map<String, DataSource> datasourceByName = new HashMap<String, DataSource>();
	public static Map<String, Organism> speciesFromTaxId = new HashMap<String, Organism>();
	public static Map<Organism, String> taxIdFromSpecies = new HashMap<Organism, String>();

	public static Set<String> directedInteractions = new HashSet<String>();

	static {
		BioDataSource.init();

		datasourceByName.put("uniprotkb", BioDataSource.UNIPROT);
		datasourceByName.put("chebi", BioDataSource.CHEBI);
		datasourceByName.put("entrez gene/locuslink", BioDataSource.ENTREZ_GENE);
		datasourceByName.put("entrezgene/locuslink", BioDataSource.ENTREZ_GENE);
		datasourceByName.put("refseq", BioDataSource.REFSEQ);
		datasourceByName.put("ensembl", BioDataSource.ENSEMBL);

		speciesFromTaxId.put("10090", Organism.MusMusculus);
		speciesFromTaxId.put("9606", Organism.HomoSapiens);

		for(Entry<String, Organism> e : speciesFromTaxId.entrySet()) {
			taxIdFromSpecies.put(e.getValue(), e.getKey());
		}

		directedInteractions.add("methylation, physical  association");
		directedInteractions.add("phosphorylation reaction");
		directedInteractions.add("methylation reaction");
		directedInteractions.add("atpase reaction");
		directedInteractions.add("phosphorylation");
		directedInteractions.add("enzymatic reaction");
		directedInteractions.add("protein cleavage");
		directedInteractions.add("deubiquitination reaction");
		directedInteractions.add("dephosphorylation reaction");
		directedInteractions.add("cleavage reaction");
		directedInteractions.add("synthetic genetic interaction defined by inequality");
		directedInteractions.add("acetylation");
		directedInteractions.add("reaction");
	}

	private Set<DataSource> commonDs = new HashSet<DataSource>();

	private Set<String> ignoreInteractions = new HashSet<String>();

	private Organism filterSpecies;
	private IDMapper idmapper;

	public PsiMiImport() {
	}

	private boolean isDirected(String interaction) {
		for(String d : directedInteractions) {
			if(interaction.contains(d)) return true;
		}
		return false;
	}

	public void setIgnoreInteractions(Collection<String> ignoreInteractions) {
		this.ignoreInteractions.clear();
		this.ignoreInteractions.addAll(ignoreInteractions);
	}

	public void setIdmapper(IDMapper idmapper) {
		this.idmapper = idmapper;
	}

	public void setFilterSpecies(Organism filterSpecies) {
		this.filterSpecies = filterSpecies;
	}

	public void setCommonDs(Set<DataSource> commonDs) {
		this.commonDs = commonDs;
	}

	public void setCommonDs(DataSource... dataSources) {
		commonDs = new HashSet<DataSource>();
		for(DataSource ds : dataSources) {
			commonDs.add(ds);
		}
	}

	private DataSource getDataSource(String name, Organism species) {
		DataSource ds = datasourceByName.get(name);
		if(BioDataSource.ENSEMBL.equals(ds)) {
			return BioDataSource.getSpeciesSpecificEnsembl(species);
		} else {
			return ds;
		}
	}

	private Xref getXref(Interactor i) {
		Xref x = null;
		List<CrossReference> allids = new ArrayList<CrossReference>(i.getIdentifiers());
		allids.addAll(i.getAlternativeIdentifiers());

		for(CrossReference cr : allids) {
			DataSource ds = getDataSource(cr.getDatabase(), speciesFromTaxId.get(i.getOrganism().getTaxid()));
			if(ds != null) {
				String id = cr.getIdentifier();

				//Hack to adapt the official ChEBI id (CHEBI:XXXX) to the bridgedb form (XXXX)
				if(BioDataSource.CHEBI.equals(ds)) {
					if(id.startsWith("CHEBI:")) {
						id = id.replace("CHEBI:", "");
					}
				}
				//Hack to remove the uniprot isoform details: Q9DBG3-1 to Q9DBG3
				if(BioDataSource.UNIPROT.equals(ds)) {
					if(id.matches(".+\\-[1-9]$")) {
						id = id.substring(0, id.lastIndexOf('-'));
					}
				}

				//Check if identifier matches expected form (because there are also symbols annotated with the same database)
				//Except for CHEBI (since the pattern does not match what's in the database)
				if(!BioDataSource.CHEBI.equals(ds) &&!DataSourcePatterns.getPatterns().get(ds).matcher(id).matches()) {
					log.warning("Ignoring identifier with wrong pattern: " + cr);
					continue;
				}

				//Hack to make intact work (adds comment in parentheses after each id :S ).
				if(BioDataSource.UNIPROT.equals(ds) && id.endsWith(")")) {
					continue;
				}

				x = new Xref(id, ds);
				//Prefer common datasource
				if(commonDs.contains(ds)) break;
			}
		}
		if(x == null) {
			log.warning("Unable to create an xref for " + i.getIdentifiers());
		}

		return x;
	}

	private String getLabel(Xref x) throws IDMapperException {
		String label = x.getId();
		if(idmapper != null && idmapper instanceof AttributeMapper) {
			SortedSet<String> symbols = new TreeSet<String>(((AttributeMapper)idmapper).getAttributes(x, "Symbol"));
			if(symbols.size() > 0) label = symbols.first();
		}
		return label;
	}

	private boolean checkSpecies(Interactor i) {
		if(filterSpecies != null) {
			Organism o = speciesFromTaxId.get(i.getOrganism().getTaxid());
			if(o != null && !filterSpecies.equals(o)) {
				log.warning("Filtering out interaction with species " + o);
				return false;
			}
		}
		return true;
	}


	private Set<Xref> mapToCommon(Xref x) throws IDMapperException {
		if(idmapper != null) {
			if(commonDs.contains(x.getDataSource())) {
				return Utils.setOf(x);
			}
			//Try to map to common datasource
			for(DataSource ds : commonDs) {
				Set<Xref> eg = idmapper.mapID(x, ds);
				if(eg.size() != 0) return eg;
			}

			log.warning("Unable to map xref " + x + " to a common datasource " + commonDs);
			return new HashSet<Xref>();
		}
		return Utils.setOf(x); //Don't map if no idmapper present
	}

	private List<String> extractText(List<?> objects) {
		List<String> result = new ArrayList<String>();
		for(Object o : objects) {
			if(o instanceof CrossReference) result.add(((CrossReference)o).getText());
			else if(o instanceof InteractionType) result.add(((InteractionType)o).getText());
			else if(o instanceof Author) result.add(((Author)o).getName());
			else throw new IllegalArgumentException("extractText not supported for class " + o.getClass());
		}
		return result;
	}

	private List<String> extractId(List<CrossReference> objects) {
		List<String> result = new ArrayList<String>();
		for(CrossReference o : objects) result.add(o.getIdentifier());
		return result;
	}

	public Network<Xref, String> createNetwork(Collection<File> inFiles) throws IOException, ConverterException, IDMapperException {
		Network<Xref, String> network = new Network<Xref, String>(new JungGraph<Xref, String>(new DirectedSparseGraph<Xref, String>()));
		Graph<Xref, String> graph = network.getGraph();

		for(File f : inFiles) {
			int iraw = 0;
			int imap = 0;
			int imap_unable = 0;
			int imap_many = 0;

			log.info("Processing " + f);
			PsimiTabReader in = new PsimiTabReader(false);
			Collection<BinaryInteraction> interactions = in.read(f);
			log.info("Number of interactions: " + interactions.size());

			for(BinaryInteraction i : interactions) {
				iraw++;

				boolean skip = false;
				for(String itxt : extractText(i.getInteractionTypes())) {
					if(ignoreInteractions.contains(itxt)) {
						log.info("Skipping interaction with type " + i.getInteractionTypes());
						skip = true;
						break;
					}
				}
				if(skip) continue;

				Interactor a = i.getInteractorA();
				Interactor b = i.getInteractorB();

				if(!checkSpecies(a) || !checkSpecies(b)) continue;
				Xref xa = getXref(a);
				Xref xb = getXref(b);
				if(xa == null || xb == null) {
					log.warning("Skipping " + i + ": missing xref");
					continue;
				}
				//Map ids to common datasource
				Set<Xref> mxa = mapToCommon(xa);
				Set<Xref> mxb = mapToCommon(xb);
				if(mxa.size() == 0 || mxb.size() == 0) {
					imap_unable++;

					log.warning("Skipping " + i + ": unable to map xref");
					continue;
				}

				if(mxa.size() > 1|| mxb.size() > 1) {
					imap_many++;
					log.info("One-to-many: " + xa + " -> " + mxa);
					log.info("One-to-many: " + xb + " -> " + mxb);
				}

				//Add an edge for each mapped combination
				for(Xref x1 : mxa) {
					for(Xref x2 : mxb) {
						imap++;

						//Add the nodes
						graph.addNode(x1);
						graph.addNode(x2);

						//Store the original xrefs
						network.setNodeAttribute(x1, AttributeKey.OriginalXref.name(), "" + xa);
						network.setNodeAttribute(x2, AttributeKey.OriginalXref.name(), "" + xb);

						//Add the edge
						String interaction = "";
						for(Object to : i.getInteractionTypes()) {
							InteractionType type = (InteractionType)to;
							interaction += type.getText() + "|";
						}
						boolean dir = isDirected(interaction);
						interaction = interaction.length() > 0 ? interaction.substring(0, interaction.length() - 1) : "";
						List<String> addedEdges = new ArrayList<String>();
						if(dir || x1.equals(x2)) {
							String edge = x1 + " - " + interaction + " > " + x2;
							graph.addEdge(edge, x1, x2);
							addedEdges.add(edge);
						} else { //Add an edge in both directions
							String edge1 = x1 + " < " + interaction + " > " + x2;
							graph.addEdge(edge1, x1, x2);
							addedEdges.add(edge1);
							String edge2 = x2 + " < " + interaction + " > " + x1;
							graph.addEdge(edge2, x2, x1);
							addedEdges.add(edge2);
						}

						for(String edge : addedEdges) {
							network.setEdgeAttribute(edge, AttributeKey.Interaction.name(), 
									StringUtils.join(extractText(i.getInteractionTypes()), ", "));
							network.setEdgeAttribute(edge, "psimi_" + AttributeKey.Interaction.name(), 
									StringUtils.join(i.getInteractionTypes(), ", "));
							network.setEdgeAttribute(edge, AttributeKey.Authors.name(), 
									StringUtils.join(extractText(i.getAuthors()), ", "));
							network.setEdgeAttribute(edge, AttributeKey.PubmedId.name(), 
									StringUtils.join(extractId(i.getPublications()), ", "));
							network.setEdgeAttribute(edge, AttributeKey.Source.name(), 
									StringUtils.join(extractText(i.getSourceDatabases()), ", "));
							network.setEdgeAttribute(edge, AttributeKey.DetectionMethod.name(), 
									StringUtils.join(extractText(i.getDetectionMethods()), ", "));
						}
					}
				}
			}

			//Write report data for this file
			String report = network.getNetworkAttribute(AttributeKey.ConversionReport.name());
			if(report == null) report = "";

			report += "== Imported file: " + f + "\n";
			report += "Mapping performed: " + (idmapper == null ? "No" : "Yes") + "\n";
			report += "Filtering for species: " + (filterSpecies == null ? "No" : "Yes (" + filterSpecies.latinName() + ")") + "\n";
			report += "Raw interactions: " + iraw + "\n";
			report += "Mapped interactions: " + imap + "\n";
			report += "Unable to map: " + imap_unable + "\n";
			report += "One-to-many mapping: " + imap_many + "\n";

			network.setNetworkAttribute(AttributeKey.ConversionReport.name(), report);

			//Add labels
			log.info("Looking up labels");
			int i = 0;
			for(Xref x : network.getGraph().getNodes()) {
				if(i % 100 == 0) log.info(i + " out of " + network.getGraph().getNodeCount());
				network.setNodeAttribute(x, AttributeKey.Label.name(), getLabel(x));
				i++;
			}
		}
		return network;
	}

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);

			PsiMiImport in = new PsiMiImport();
			if(pargs.getSpecies() != null) {
				Organism o = Organism.fromCode(pargs.getSpecies());
				if(o == null) o = Organism.fromLatinName(pargs.getSpecies());
				if(o != null) in.setFilterSpecies(o);
			}
			in.setIdmapper(didm.getIDMapper());
			in.setCommonDs(didm.getDataSources());

			Set<File> files = new HashSet<File>();
			for(File f : pargs.getIn()) {
				if(f.isDirectory()) files.addAll(FileUtils.getFiles(f, "txt", true));
				else files.add(f);
			}
			Network<Xref, String> n = in.createNetwork(files);
			PrintWriter out = new PrintWriter(pargs.getOut());
			if(pargs.getOut().getName().endsWith("gml")) {
				GmlWriter.writeGml(out, n);
			} else {
				n.writeToXGMML(out);
			}
			out.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private interface Args extends AHelp, AIDMapper {
		@Option(shortName = "i", description = "The PSI-TAB input files")
		List<File> getIn();
		@Option(shortName = "o", description = "The gml or xgmml file to write the converted network to")
		File getOut();
		@Option(description = "The species to filter for")
		String getSpecies();
	}
}