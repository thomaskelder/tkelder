package org.pct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.model.Graph.GraphFactory;
import org.pct.util.ArgsData;
import org.pct.util.ArgsParser;
import org.pct.util.GraphSampler;
import org.pct.util.RStats;
import org.pct.util.ArgsData.DCrossTalk;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DNetworks;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsData.DWeights;
import org.pct.util.ArgsParser.ACrossTalk;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.ANetwork;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;
import org.pct.util.RStats.FisherStats;
import org.pct.util.XrefData.WeightProvider;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class PathwayCrossTalk {
	private final static Logger log = Logger.getLogger(PathwayCrossTalk.class.getName());

	private boolean performFisher = true;
	private int nperm = 1000;
	private boolean omitOverlap = false;
	private int nthreads = Runtime.getRuntime().availableProcessors();

	private IDMapper idmapper;

	private GraphFactory<String, String> pathwayGraphFactory;

	private Map<String, WeightProvider<Xref>> weightsMap = new HashMap<String, WeightProvider<Xref>>();

	public PathwayCrossTalk() {
		pathwayGraphFactory = new GraphFactory<String, String>() {
			public Graph<String, String> createGraph() {
				return new JungGraph<String, String>(new UndirectedSparseGraph<String, String>());
			}
		};
		//Set default edge weights to 1
		setWeights(null);
	}

	public void setIdmapper(IDMapper idmapper) {
		this.idmapper = idmapper;
	}

	public void setNumberThreads(int nthreads) {
		this.nthreads = nthreads;
	}

	public void setOmitOverlap(boolean omitOverlap) {
		this.omitOverlap = omitOverlap;
	}

	public void setWeights(WeightProvider<Xref> weights) {
		this.weightsMap.clear();
		if(weights != null) {
			this.weightsMap.put("", weights);
		} else {
			//Default, gives all edges weight of 1
			weightsMap.put("", new WeightProvider<Xref>() {
				public double getWeight(Xref n1, Xref n2) { return 1; }
			});
		}
	}

	public void setWeightsMap(Map<String, WeightProvider<Xref>> weightsMap) {
		this.weightsMap = weightsMap;
	}

	public void setPerformFisher(boolean performFisher) {
		this.performFisher = performFisher;
	}

	public void setPathwayGraphFactory(
			GraphFactory<String, String> pathwayGraphFactory) {
		this.pathwayGraphFactory = pathwayGraphFactory;
	}

	public IDMapper getIdmapper() {
		return idmapper;
	}

	public void setNperm(int nperm) {
		this.nperm = nperm;
	}

	/**
	 * Create a cross-talk network between the pathways based on the given xref interaction network.
	 * Note that both the pathway and interaction xrefs should be in the same identifier system (no extra
	 * mapping will be performed).
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public Network<String, String> createCrossTalkNetwork(Map<String, Set<Xref>> pathways, Network<Xref, String> interactions) throws IDMapperException, IOException, InterruptedException {
		//Find initial counts of interactions between pathways
		List<String> orderedNames = new ArrayList<String>(pathways.keySet());
		final int npws = orderedNames.size();

		log.info("Removing xrefs without interactions from working set");
		Map<String, Set<Xref>> intPathways = new HashMap<String, Set<Xref>>();
		for(String pn : pathways.keySet()) {
			Set<Xref> xrefs = new HashSet<Xref>();
			for(Xref x : pathways.get(pn)) {
				if(interactions.getGraph().containsNode(x) && 
						interactions.getGraph().getNeighborCount(x) > 0) xrefs.add(x);
			}
			intPathways.put(pn, xrefs);
		}

		log.info("Listing xref neighbours from interaction network");
		Set<Xref> allXrefs = new HashSet<Xref>();
		for(Set<Xref> xrefs : intPathways.values()) allXrefs.addAll(xrefs);
		allXrefs.addAll(interactions.getGraph().getNodes());


		log.info("Counting initial links between pathways");
		Map<Xref, Set<Xref>> neighbours = getNeighbours(allXrefs, interactions, null);
		int[][] counts = countLinks(intPathways, orderedNames, neighbours);

		Map<String, Map<String, Set<Xref>>> pathwayMap = new HashMap<String, Map<String,Set<Xref>>>();
		Map<String, int[][]> largerMap = new HashMap<String, int[][]>();
		Map<String, double[][]> pMap = new HashMap<String, double[][]>();
		Map<String, FisherStats[][]> fpMap = new HashMap<String, FisherStats[][]>();
		Map<String, double[][]> scorePermSumMap = new HashMap<String, double[][]>();
		Map<String, GraphSampler<Xref>> interactionSamplers = new HashMap<String, GraphSampler<Xref>>();
		Map<String, double[][]> scoreMap = new HashMap<String, double[][]>();
		Map<String, Map<Xref, Set<Xref>>> neighbourMap = new HashMap<String, Map<Xref,Set<Xref>>>();
		for(String s : weightsMap.keySet()) {
			Map<Xref, Set<Xref>> nbs = getNeighbours(allXrefs, interactions, weightsMap.get(s));
			neighbourMap.put(s, nbs);
			
			pathwayMap.put(s, removeNoNeighbours(intPathways, nbs));
			largerMap.put(s, new int[npws][npws]);
			pMap.put(s, new double[npws][npws]);
			fpMap.put(s, new FisherStats[npws][npws]);
			scorePermSumMap.put(s, new double[npws][npws]);
			GraphSampler<Xref> interactionsSampler = new GraphSampler<Xref>(
					interactions.getGraph(), weightsMap.get(s));
			interactionSamplers.put(s, interactionsSampler);
			scoreMap.put(s, countScores(intPathways, orderedNames, nbs, weightsMap.get(s)));
		}

		PermutationData pdata = new PermutationData();
		pdata.largerMap = largerMap;
		pdata.neighbourMap = neighbourMap;
		pdata.orderedPathwayNames = orderedNames;
		pdata.pathwayMap = pathwayMap;
		pdata.samplerMap = interactionSamplers;
		pdata.scoreMap = scoreMap;
		pdata.scorePermSumMap = scorePermSumMap;

		//Start permutation and calculate p-values
		log.info("Permuting " + nperm + " times...");
		runPermutations(pdata);

		//Calculate p-values based on the permutation
		for(String s : weightsMap.keySet()) {
			double[][] pv = pMap.get(s);
			int[][] lg = largerMap.get(s);
			for(int x = 0; x < npws; x++) {
				for(int y = x + 1; y < npws; y++) {
					pv[x][y] = (double)lg[x][y] / (double)nperm;
				}
			}
		}

		//Calculate p-values based on Fisher's exact test
		if(performFisher) {
			List<FisherStats> stats = new ArrayList<FisherStats>();
			for(String s : weightsMap.keySet()) {
				log.info("Performing Fisher's exact test for " + s);
				double N = 0; //Total interactions between pathway pairs
				double R = 0; //Average of total interactions between pathways pairs over all permutations

				double[][] scorePermSum = scorePermSumMap.get(s);
				double[][] scores = scoreMap.get(s);
				FisherStats[][] fpv = fpMap.get(s);

				//Calculate N and R
				for(int x = 0; x < npws; x++) {
					for(int y = x + 1; y < npws; y++) {
						N += scores[x][y];
						R += scorePermSum[x][y] / nperm;
					}
				}

				//Gather data for Fisher's exact test for each pathway pair
				for(int x = 0; x < npws; x++) {
					for(int y = x + 1; y < npws; y++) {
						double n = scores[x][y]; //Interactions between pathways x and y
						double r = 0; //Average interactions between pathways x and y over all permutations
						for(int i = 0; i < nperm; i++) {
							r += scorePermSum[x][y] / nperm;
						} 
						r /= nperm;

						int a = (int)Math.round(n);
						int b = (int)Math.round(N - n);
						int c = (int)Math.round(r);
						int d = (int)Math.round(R - r);
						FisherStats fs = new FisherStats(a, b, c, d);
						fpv[x][y] = fpv[y][x] = fs;
						stats.add(fs);
					}
				}

				//Calculate p and q-values through R
				RStats.performFisher(stats);
			}
		}

		//Calculate overlap between pathways
		log.info("Calculating overlap between pathways");
		double[][] overlap = calculateOverlap(orderedNames, pathways);

		//Create a network
		log.info("Creating cross-talk network");
		Network<String, String> network = new Network<String, String>(pathwayGraphFactory.createGraph());
		Graph<String, String> graph = network.getGraph();
		for(int x = 0; x < npws; x++) {
			String px = orderedNames.get(x);
			graph.addNode(px);
			network.setNodeAttribute(px, AttributeKey.NrXrefs.name(), "" + pathways.get(px).size());
			network.setNodeAttribute(px, AttributeKey.PathwayId.name(), "" + px);
			for(int y = x + 1; y < npws; y++) {
				String py = orderedNames.get(y);
				graph.addNode(py);
				network.setNodeAttribute(py, AttributeKey.NrXrefs.name(), "" + pathways.get(py).size());
				network.setNodeAttribute(py, AttributeKey.PathwayId.name(), "" + py);

				String edge = px + " <-> " + py;
				graph.addEdge(edge, px, py);
				network.setEdgeAttribute(edge, AttributeKey.InteractionCount.name(), "" + counts[x][y]);
				network.setEdgeAttribute(edge, AttributeKey.Overlap.name(), "" + overlap[x][y]);

				for(String s : weightsMap.keySet()) {
					String post = "".equals(s) ? s : ("_" + s);
					double[][] scores = scoreMap.get(s);
					double[][] pvalues = pMap.get(s);
					FisherStats[][] fpvalues = fpMap.get(s);
					network.setEdgeAttribute(edge, AttributeKey.InteractionScore.name() + post, "" + scores[x][y]);
					network.setEdgeAttribute(edge, AttributeKey.Pvalue.name() + post, "" + pvalues[x][y]);
					if(performFisher) {
						double odds = fpvalues[x][y].odds;
						if(Double.isInfinite(odds)) odds = -1;
						network.setEdgeAttribute(edge, AttributeKey.FisherPvalue.name() + post, "" + fpvalues[x][y].p);
						network.setEdgeAttribute(edge, AttributeKey.FisherQvalue.name() + post, "" + fpvalues[x][y].q);
						network.setEdgeAttribute(edge, AttributeKey.FisherOdds.name() + post, "" + fpvalues[x][y].odds);
						network.setEdgeAttribute(edge, AttributeKey.Fishern.name() + post, "" + fpvalues[x][y].a);
						network.setEdgeAttribute(edge, AttributeKey.Fisherr.name() + post, "" + fpvalues[x][y].c);
						network.setEdgeAttribute(edge, AttributeKey.FisherNn.name() + post, "" + fpvalues[x][y].b);
						network.setEdgeAttribute(edge, AttributeKey.FisherRr.name() + post, "" + fpvalues[x][y].d);

					}
				}
			}
		}

		network.setTitle("crosstalk-" + new Date());
		return network;
	}

	private static double[][] calculateOverlap(List<String> orderedNames, Map<String, Set<Xref>> pathways) {
		double[][] overlap = new double[pathways.size()][pathways.size()];

		for(int x = 0; x < pathways.size(); x++) {
			Set<Xref> px = pathways.get(orderedNames.get(x));
			for(int y = x + 1; y < pathways.size(); y++) {
				Set<Xref> py = new HashSet<Xref>(pathways.get(orderedNames.get(y)));
				double size = Math.min(px.size(), py.size());
				py.retainAll(px);
				//Calculate percentage overlap
				double o = py.size() / size;
				overlap[x][y] = o;
			}
		}
		return overlap;
	}

	private Map<String, Set<Xref>> removeNoNeighbours(Map<String, Set<Xref>> pathways, Map<Xref, Set<Xref>> neighbours) {
		Map<String, Set<Xref>> fpathways = new HashMap<String, Set<Xref>>();
		for(String n : pathways.keySet()) {
			Set<Xref> p = pathways.get(n);
			Set<Xref> fp = new HashSet<Xref>(p);
			for(Xref x : p) if(!neighbours.containsKey(x)) fp.remove(x);
			fpathways.put(n, fp);
		}
		return fpathways;
	}
	
	private static Map<Xref, Set<Xref>> getNeighbours(Set<Xref> xrefs, Network<Xref, String> interactions, WeightProvider<Xref> weights) {
		Map<Xref, Set<Xref>> neighbours = new HashMap<Xref, Set<Xref>>();
		for(Xref x : xrefs) {
			Set<Xref> nbs = new HashSet<Xref>();
			Collection<Xref> nb = interactions.getGraph().getNeighbors(x);
			if(nb != null) {
				if(weights != null) {
					for(Xref nx : nb) {
						if(weights.getWeight(x, nx) > 0) nbs.add(nx);
					}
				} else {
					nbs.addAll(nb);
				}
			}
			if(nbs.size() > 0) neighbours.put(x, nbs);
		}
		return neighbours;
	}

	private int[][] countLinks(Map<String, Set<Xref>> pathways, List<String> orderedNames, Map<Xref, Set<Xref>> neighbours) {
		int[][] counts = new int[orderedNames.size()][orderedNames.size()];

		for(int i = 0; i < orderedNames.size(); i++) {
			Set<Xref> pwXrefsI = pathways.get(orderedNames.get(i));
			
			for(int j = i + 1; j < orderedNames.size(); j++) {
				Set<Xref> pwXrefsJ = pathways.get(orderedNames.get(j));

				int count = 0;
				for(Xref x : pwXrefsI) {
					if(omitOverlap && pwXrefsJ.contains(x)) continue;
					
					Set<Xref> nx = neighbours.get(x);
					if(nx != null) for(Xref y : pwXrefsJ) {
						if(nx.contains(y)) {
							if(!(pwXrefsI.contains(y) && (omitOverlap || pwXrefsJ.contains(x)))) count++;
						}
					}
				}
				counts[i][j] = counts[j][i] = count;
			}
		}
		return counts;
	}

	private double[][] countScores(Map<String, Set<Xref>> pathways, List<String> orderedNames, Map<Xref, Set<Xref>> neighbours, WeightProvider<Xref> weights) {
		double[][] scores = new double[orderedNames.size()][orderedNames.size()]; 

		for(int i = 0; i < orderedNames.size(); i++) {
			Set<Xref> pwXrefsI = pathways.get(orderedNames.get(i));
			
			for(int j = i + 1; j < orderedNames.size(); j++) {
				Set<Xref> pwXrefsJ = pathways.get(orderedNames.get(j));

				double score = 0;
				for(Xref x : pwXrefsI) {
					
					Set<Xref> nx = neighbours.get(x);

					if(omitOverlap && pwXrefsJ.contains(x)) continue;
					if(nx == null) continue;
					
					for(Xref y : pwXrefsJ) {
						if(nx.contains(y)) {
							if(!(pwXrefsI.contains(y) && (omitOverlap || pwXrefsJ.contains(x)))) {
								score += weights.getWeight(x, y);
							}
						}
					}
				}
				scores[i][j] = scores[j][i] = score;
			}
		}
		return scores;
	}

	private Map<String, Set<Xref>> shuffleByXrefs(GraphSampler<Xref> sampler, Map<String, Set<Xref>> pathways) {
		Map<String, Set<Xref>> shuffled = new HashMap<String, Set<Xref>>();
		for(String p : pathways.keySet()) {
			Set<Xref> newXrefs = new HashSet<Xref>();
			for(Xref x : pathways.get(p)) newXrefs.add(sampler.sampleNode(x));
			shuffled.put(p, newXrefs);
		}
		return shuffled;
	}

	private void runPermutations(PermutationData data) throws InterruptedException {
		CountDownLatch progress = new CountDownLatch(nthreads);
		ExecutorService e = Executors.newFixedThreadPool(nthreads);

		int part = nperm / nthreads;
		if(part < 1) part = 1;
		for(int i = 0; i < nthreads; i++) {
			int start = i * part;
			int end = i * part + part - 1;
			if(i == nthreads - 1) end = nperm - 1;
			log.info("Starting thread for permutation " + start + " to " + end);
			e.execute(new PermutationWorker(start, end, progress, data));
		}
		progress.await();
		e.shutdown();
	}

	private class PermutationWorker implements Runnable {
		int start, end;
		PermutationData data;
		CountDownLatch progress;

		public PermutationWorker(int start, int end, CountDownLatch progress, PermutationData data) {
			this.start = start;
			this.end = end;
			this.data = data;
			this.progress = progress;
		}

		public void run() {
			int npws = data.orderedPathwayNames.size();
			for(int i = start; i <= end; i++) {
				log.info("...permutation " + i);

				//Sample genes in pathways from interaction network (by similar degree)
				Map<String, double[][]> shuffledScoreMap = new HashMap<String, double[][]>();

				for(String s : weightsMap.keySet()) {
					Map<String, Set<Xref>> shuffledXrefs = shuffleByXrefs(data.samplerMap.get(s), data.pathwayMap.get(s));
					double[][] scores = countScores(
							shuffledXrefs, data.orderedPathwayNames, data.neighbourMap.get(s), weightsMap.get(s)
					);
					shuffledScoreMap.put(s, scores);
				}

				for(String s : weightsMap.keySet()) {
					double[][] shuffledScores = shuffledScoreMap.get(s);
					double[][] scores = data.scoreMap.get(s);
					double[][] scorePermSum = data.scorePermSumMap.get(s);
					int[][] larger = data.largerMap.get(s);
					for(int x = 0; x < npws; x++) {
						for(int y = x + 1; y < npws; y++) {
							larger[x][y] += shuffledScores[x][y] >= scores[x][y] ? 1 : 0;
							scorePermSum[x][y] += shuffledScores[x][y];
						}
					}
				}
			}
			progress.countDown();
		}
	}

	private class PermutationData {
		Map<String, GraphSampler<Xref>> samplerMap;
		List<String> orderedPathwayNames;

		Map<String, Map<String, Set<Xref>>> pathwayMap;
		Map<String, Map<Xref, Set<Xref>>> neighbourMap;
		Map<String, double[][]> scoreMap;
		Map<String, int[][]> largerMap;
		Map<String, double[][]> scorePermSumMap;
	}

	public interface Args extends AIDMapper, AWeights, APathways, ACrossTalk, ANetwork, AHelp {
		@Option(shortName = "o", description = "The output file.")
		public File getOut();
	}

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			DCrossTalk dct = new DCrossTalk(pargs, didm);
			DWeights dw = new DWeights(pargs, didm);
			DNetworks<Xref, String> dnw = ArgsData.loadCrossTalkInteractions(pargs);

			PathwayCrossTalk ct = dct.getPathwayCrossTalk();
			ct.setWeights(dw.getWeightProvider());

			log.info("Creating cross-talk network");
			Network<String, String> ctn = ct.createCrossTalkNetwork(dpws.getPathways(), dnw.getMergedNetwork());

			//Add additional pathway attributes
			log.info("Adding additional pathway attributes");
			addPathwayAttributes(ctn, dpws.getPathwayFiles());

			//Add command line arguments for future reference
			String as = '"' + StringUtils.join(args, "\" \"") + '"';
			ctn.setNetworkAttribute(AttributeKey.Args.name(), as);
			
			log.info("Writing cross-talk network to " + pargs.getOut());
			ctn.setTitle(pargs.getOut().getName());
			FileWriter out = new FileWriter(pargs.getOut());
			ctn.writeToXGMML(out);
			out.close();
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
			e.printStackTrace();
		}
	}

	public static void addPathwayAttributes(Network<String, String> pathwayNetwork, Collection<File> pathwayFiles) throws ConverterException {
		log.info("Adding additional pathway attributes");
		for(File f : pathwayFiles) {
			List<String> ids = new ArrayList<String>();

			String fname = f.getName();

			log.fine("Processing " + fname);
			boolean merged = false;
			if(pathwayNetwork.getGraph().containsNode(fname)) {
				ids.add(fname);
			} else {
				log.fine("Network doesn't contain node with id " + fname);

				//Try to find out if this pathway is part of merged pathway
				String mf = null;
				for(String n : pathwayNetwork.getGraph().getNodes()) {
					if(n.contains(fname)) {
						mf = n;
						merged = true;
						log.fine("Id belongs to merged node: " + mf);
						ids.add(mf);
					}
				}
			}

			Pathway p = new Pathway();
			p.readFromXml(f, false);
			String name = p.getMappInfo().getMapInfoName();
			Set<Xref> unmappedXrefs = new HashSet<Xref>(p.getDataNodeXrefs());

			for(String id : ids) {
				String label = pathwayNetwork.getNodeAttribute(id, AttributeKey.Label.name());
				if(name.equals(label)) label = null;
				label = label == null ? name : label + " - " + name;
				log.info("Setting label to: " + label);
				pathwayNetwork.setNodeAttribute(id, AttributeKey.Label.name(), label);

				String nx = pathwayNetwork.getNodeAttribute(id, AttributeKey.NrUnmappedXrefs.name());
				pathwayNetwork.setNodeAttribute(id, AttributeKey.NrUnmappedXrefs.name(), 
						"" + (nx == null ? unmappedXrefs.size() : unmappedXrefs.size() + Double.parseDouble(nx))
				);

				pathwayNetwork.setNodeAttribute(id, AttributeKey.Merged.name(), "" + merged);
			}
		}
	}

	public static final GraphFactory<Xref, String> defaultInteractionFactory = new GraphFactory<Xref, String>() {
		public Graph<Xref, String> createGraph() {
			//return new JgtGraph<Xref, String>(new Pseudograph<Xref, String>(String.class));
			return new JungGraph<Xref, String>(new UndirectedSparseGraph<Xref, String>());
		}
	};
	public static final GraphFactory<String, String> defaultPathwayFactory = new GraphFactory<String, String>() {
		public Graph<String, String> createGraph() {
			//return new JgtGraph<String, String>(new Pseudograph<String, String>(String.class));
			return new JungGraph<String, String>(new UndirectedSparseGraph<String, String>());
		}
	};
}
