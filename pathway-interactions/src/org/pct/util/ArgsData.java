package org.pct.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.Utils;
import org.pathvisio.util.PathwayParser.ParseException;
import org.pct.PathwayCrossTalk;
import org.pct.io.PathwayUtils;
import org.pct.io.PathwayUtils.PathwayFilter;
import org.pct.model.Network;
import org.pct.model.Graph.GraphFactory;
import org.pct.model.Network.FromString;
import org.pct.util.ArgsParser.ACrossTalk;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.ANetwork;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;
import org.pct.util.XrefData.WeightProvider;
import org.xml.sax.SAXException;

/**
 * Data objects commonly used by scripts in this package. This class makes
 * it easier to share, parse and load them.
 * 
 * @author thomas
 */
public class ArgsData {
	private final static Logger log = Logger.getLogger(ArgsData.class.getName());

	public static DNetworks<Xref, String> loadCrossTalkInteractions(ANetwork anw) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
		return new DNetworks<Xref, String>(
			anw, PathwayCrossTalk.defaultInteractionFactory, Network.xrefFactory, 
			Network.defaultFactory, false, false
		);
	}
	
	public static class DNetworks<N, E> {
		boolean multiMerge = true;
		private GraphFactory<N,E> graphFactory;
		private Map<File, Network<N, E>> networks;
		private Network<N, E> merged;
		
		public DNetworks(ANetwork anw, GraphFactory<N, E> graphFactory, 
				FromString<N> nodeFactory, FromString<E> edgeFactory, boolean readAttributes, boolean multiMerge) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
			this.multiMerge = multiMerge;
			this.graphFactory = graphFactory;
			loadNetworks(anw, nodeFactory, edgeFactory, readAttributes);
		}
		
		public DNetworks(ANetwork anw, GraphFactory<N, E> graphFactory, 
				FromString<N> nodeFactory, FromString<E> edgeFactory, boolean readAttributes) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
			this(anw, graphFactory, nodeFactory, edgeFactory, readAttributes, true);
		}
		
		public Map<File, Network<N, E>> getNetworks() {
			return networks;
		}
		
		public Network<N, E> getMergedNetwork() {
			if(merged == null) {
				merged = new Network<N, E>(graphFactory.createGraph());
				for(Network<N, E> n : networks.values()) {
					merged.merge(n, multiMerge);
				}
			}
			return merged;
		}
		
		private void loadNetworks(ANetwork anw, FromString<N> nodeFactory, 
				FromString<E> edgeFactory, boolean readAttributes) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
			log.info("Reading networks");
			networks = new HashMap<File, Network<N, E>>();
			Set<File> xgmmlFiles = new HashSet<File>();
			for(File f : anw.getNetworks()) {
				xgmmlFiles.addAll(FileUtils.getFiles(f, "xgmml", true));
			}
			for(File f : xgmmlFiles) {
				log.fine("Reading " + f);
				Network<N, E> n = new Network<N, E>(graphFactory.createGraph());
				n.readFromXGMML(
						new FileReader(f), nodeFactory, edgeFactory, readAttributes
				);
				networks.put(f, n);
			}
		}
	}
	
	public static class DPathways {
		private Map<String, Set<Xref>> pathwaysByMergedId;
		private Map<Set<String>, Set<Xref>> pathwaysById;
		private Set<File> pathwayFiles;
		private Map<String, File> id2file;
		
		public DPathways(APathways apws, DIDMapper didm) throws SAXException, ParseException, IDMapperException {
			loadPathways(apws, didm);
		}

		public Set<File> getPathwayFiles() {
			return pathwayFiles;
		}

		public Map<String, Set<Xref>> getPathways() {
			return pathwaysByMergedId;
		}

		public Map<Set<String>, Set<Xref>> getPathwaysById() {
			return pathwaysById;
		}
		
		public File getFile(String name) {
			return id2file.get(name);
		}
		
		private void loadPathways(APathways apws, DIDMapper didm) throws SAXException, ParseException, IDMapperException {
			log.info("Reading pathways");
			pathwayFiles = new HashSet<File>();
			for(File f : apws.getPathway()) {
				pathwayFiles.addAll(FileUtils.getFiles(f, "gpml", true));
			}
			log.info("Filtering by KEGG type");
			if(apws.isExclKeggDisease()) {
				pathwayFiles.removeAll(KeggFilter.findDiseasePathways(pathwayFiles));
			}
			if(apws.isExclKeggMeta()) {
				pathwayFiles.removeAll(KeggFilter.findOverviewPathways(pathwayFiles));
			}

			pathwaysByMergedId = GpmlUtils.readPathwaySets(
					pathwayFiles, false, didm.getIDMapper(), didm.getDataSources()
			);

			id2file = new HashMap<String, File>();
			for(File f : pathwayFiles) {
				id2file.put(f.getName(), f);
			}
			
			log.info("Filtering pathway by size");
			pathwaysByMergedId = PathwayUtils.filterPathways(
					pathwaysByMergedId,
					new PathwayFilter().setMaxXrefs(apws.getMaxPwSize()).setMinXrefs(apws.getMinPwSize())
			);
			
			if(apws.getMergeThreshold() > 0 && apws.getMergeThreshold() <= 1) {
				log.info("Merging pathway by xref overlap, threshold: " + apws.getMaxPwSize());
				pathwaysById = PathwayOverlap.createMergedPathways(
						pathwaysByMergedId, apws.getMergeThreshold());
				//Replace pathways with merged set
				pathwaysByMergedId = new HashMap<String, Set<Xref>>();
				for(Set<String> m : pathwaysById.keySet()) {
					String name = "";
					for(String nm : m) name += nm + PathwayOverlap.MERGE_SEP;
					name = name.substring(0, name.length() - PathwayOverlap.MERGE_SEP.length());
					pathwaysByMergedId.put(name, pathwaysById.get(m));
				}
			} else {
				pathwaysById = new HashMap<Set<String>, Set<Xref>>();
				for(String n : pathwaysByMergedId.keySet()) {
					pathwaysById.put(Utils.setOf(n), pathwaysByMergedId.get(n));
				}
			}
		}
	}

	public static class DIDMapper {
		private IDMapper idm;
		private DataSource[] dataSources;

		public DIDMapper(AIDMapper aidm) throws IDMapperException, ClassNotFoundException {
			loadIDMapper(aidm);
		}

		public DataSource[] getDataSources() {
			return dataSources;
		}

		public IDMapper getIDMapper() {
			return idm;
		}

		private void loadIDMapper(AIDMapper aidm) throws IDMapperException, ClassNotFoundException {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			Class.forName("org.bridgedb.rdb.IDMapperRdb");
			BioDataSource.init();

			log.info("Connecting to idmappers");
			if(aidm.isIdm()) {
				IDMapperStack idms = new IDMapperStack();
				for(String c : aidm.getIdm()) {
					log.info("Connecting to " + c);
					idms.addIDMapper(c);
					idm = idms;
				}
			}

			ArrayList<DataSource> dslist = new ArrayList<DataSource>();
			for(String c : aidm.getDs()) {
				DataSource d = DataSource.getBySystemCode(c);
				if(d == null) throw new IllegalArgumentException("Unable to find datasource for system code: " + c);
				dslist.add(d);
			}
			dataSources = dslist.toArray(new DataSource[dslist.size()]);
		}
	}

	public static class DCrossTalk {
		private PathwayCrossTalk pct;
		private Network<Xref, String> interactions;

		public DCrossTalk(ACrossTalk act, DIDMapper didm) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
			loadCrossTalk(act, didm);
		}

		public PathwayCrossTalk getPathwayCrossTalk() {
			return pct;
		}

		private void loadCrossTalk(ACrossTalk act, DIDMapper didm) {
			pct = new PathwayCrossTalk();
			pct.setNperm(act.getNperm());
			pct.setOmitOverlap(act.isOmitOverlap());
			pct.setPerformFisher(act.isFisher());
			pct.setNumberThreads(act.getNt());
			pct.setIdmapper(didm.getIDMapper());
		}
	}

	public static class DWeights {
		private Map<Xref, Double> weightValues;
		private Map<Xref, Double> transformedWeightValues;
		private WeightProvider<Xref> weightProvider;

		public DWeights(AWeights aw, DIDMapper didm) throws IOException, IDMapperException {
			loadWeights(aw, didm);
		}

		public WeightProvider<Xref> getWeightProvider() {
			if(weightProvider == null) {
				final Map<Xref, Double> wv = getTransformedWeightValues();
				weightProvider = new WeightProvider<Xref>() {
					public double getWeight(Xref n1, Xref n2) {
						double w1 = wv.containsKey(n1) ? wv.get(n1) : 0;
						double w2 = wv.containsKey(n2) ? wv.get(n2) : 0;
						return (w1 + w2) / 2.0;
					}
				};
			}
			return weightProvider;
		}

		public Map<Xref, Double> getWeightValues() {
			return weightValues;
		}

		public Map<Xref, Double> getTransformedWeightValues() {
			if(transformedWeightValues == null) {
				transformedWeightValues = XrefData.transformWeights(weightValues); 
			}
			return transformedWeightValues;
		}
		
		private void loadWeights(AWeights aw, DIDMapper didm) throws IOException, IDMapperException {
			weightValues = new HashMap<Xref, Double>();
			if(aw.isWeights()) {
				log.info("Reading weights file " + aw.getWeights());
				DataSource fds = DataSource.getBySystemCode(aw.getWeightsSys());
				weightValues.putAll(XrefData.readWeights(
						aw.getWeights(), didm.getIDMapper(), aw.isWeightsHeader(), fds,
						aw.getWeightsCol(), aw.getIdCol(), aw.getSysCol(), didm.getDataSources()));
			}
		}
	}
}
