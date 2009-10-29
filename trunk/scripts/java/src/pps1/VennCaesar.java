package pps1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;

import venn.BallVennDiagram;
import venn.NumberVennDiagram;
import venn.VennData;


/**
 * Venn diagrams based on metacore pathway analysis results for Robert Caesar.
 * @author thomas
 *
 */
public class VennCaesar {
	public static void main(String[] args) {
		VennCaesar main = new VennCaesar();
		try {
//			main.maps();
//			main.go();
//			main.mapsMet();
//			main.goMet();
			main.newGo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	File goOboFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/gene_ontology_edit.obo");
	
	File inDir = new File("/home/thomas/projects/pps1/metacore_caesar");
	File outDir = new File(inDir, "venn");
	
	String[] tissues = new String[] {
			"SAT", "MAT", "EAT"
	};
	
	VennCaesar() {
		outDir.mkdirs();
	}
	
	void maps() throws IOException {
		List<Set<String>> sets = new ArrayList<Set<String>>();
		double p = 0.05;
		
		for(String tis : tissues) {
			Set<String> s = readMetacoreOutput(
				new File(inDir, "P-values PPS1 maps and processes.xls_" + tis + "_maps.txt"),
				p, false
			);
			sets.add(s);
		}
		
		VennData<String> vdata = new VennData<String>(sets);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setLabels(tissues);
		venn.setTitle("Metacore pathways with p <= " + p);
		venn.saveImage(new File(outDir, "venn_maps_balls.png"), "png");
		
		NumberVennDiagram nvenn = new NumberVennDiagram(vdata);
		nvenn.setLabels(tissues);
		nvenn.setTitle("Metacore pathways with p <= " + p);
		nvenn.saveImage(new File(outDir, "venn_maps_number.png"), "png");
	}
	
	void mapsMet() throws IOException {
		List<Set<String>> sets = new ArrayList<Set<String>>();
		double p = 0.05;
		
		for(String tis : tissues) {
			Set<String> s = readMetacoreOutput(
				new File(inDir, "P-values PPS1 maps and processes.xls_" + tis + "_maps.txt"),
				p, true
			);
			sets.add(s);
		}
		
		VennData<String> vdata = new VennData<String>(sets);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setLabels(tissues);
		venn.setTitle("Metacore metabolic pathways with p <= " + p);
		venn.saveImage(new File(outDir, "venn_maps_met_balls.png"), "png");
		
		NumberVennDiagram nvenn = new NumberVennDiagram(vdata);
		nvenn.setLabels(tissues);
		nvenn.setTitle("Metacore metabolic pathways with p <= " + p);
		nvenn.saveImage(new File(outDir, "venn_maps_met_number.png"), "png");
	}
	
	void go() throws IOException {
		List<Set<String>> sets = new ArrayList<Set<String>>();
		double p = 0.05;
		
		for(String tis : tissues) {
			Set<String> s = readMetacoreOutput(
				new File(inDir, "P-values PPS1 maps and processes.xls_" + tis + "_GO.txt"),
				p, false
			);
			sets.add(s);
		}
		
		VennData<String> vdata = new VennData<String>(sets);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setLabels(tissues);
		venn.setTitle("Metacore GO terms with p <= " + p);
		venn.saveImage(new File(outDir, "venn_GO_balls.png"), "png");
		
		NumberVennDiagram nvenn = new NumberVennDiagram(vdata);
		nvenn.setLabels(tissues);
		nvenn.setTitle("Metacore GO terms with p <= " + p);
		nvenn.saveImage(new File(outDir, "venn_GO_number.png"), "png");
	}
	
	void goMet() throws IOException {
		List<Set<String>> sets = new ArrayList<Set<String>>();
		double p = 0.05;
		
		for(String tis : tissues) {
			Set<String> s = readMetacoreOutput(
				new File(inDir, "P-values PPS1 maps and processes.xls_" + tis + "_GO.txt"),
				p, true
			);
			sets.add(s);
		}
		
		VennData<String> vdata = new VennData<String>(sets);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setLabels(tissues);
		venn.setTitle("Metacore metabolic GO terms with p <= " + p);
		venn.saveImage(new File(outDir, "venn_GO_met_balls.png"), "png");
		
		NumberVennDiagram nvenn = new NumberVennDiagram(vdata);
		nvenn.setLabels(tissues);
		nvenn.setTitle("Metacore metabolic GO terms with p <= " + p);
		nvenn.saveImage(new File(outDir, "venn_GO_met_number.png"), "png");
	}
	
	Set<String> readMetacoreOutput(File file, double maxP, boolean metabolic) throws IOException {
		Set<String> pathways = new HashSet<String>();
		
		int nameCol = 0;
		int pCol = 3;
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = in.readLine(); //Header
		
		int metCol = 4; //for GO, contains m if metabolic
		if(metabolic) {
			String[] headers = line.split("\t");
			if("Map Folders".equals(headers[1])) {
				metCol = 1; //for pathway, contains folder
			}
		}
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t");
			double p = Double.parseDouble(cols[pCol]);
			if(p <= maxP) {
				if(metabolic) { //only metabolic pathways
					boolean include = false;
					String s = cols.length > metCol ? cols[metCol] : "";
					if(s.equals("m")) include = true;
					if(s.contains("metabolism")) include = true;
					if(s.contains("Metabolic maps")) include = true;
					if(!include) {
						System.out.println("Excluding " + cols[nameCol]);
						continue;
					}
				}
				pathways.add(cols[nameCol]);
			}
		}
		in.close();
		return pathways;
	}
	
	void newGo() throws IOException {
		double p = 0.001;
		DecimalFormat fm = new DecimalFormat("0.###E0");

		GOTree goTree = GOReader.readGOTree(goOboFile);
		GOResultParser parser = new GOResultParser(new File(inDir, "All GO three depots.txt"), goTree);
		
		List<Set<String>> sigSets = new ArrayList<Set<String>>();
		for(String tis : tissues) {
			sigSets.add(extractSignificantTerms(parser.getResult(tis).values(), p, false));
		}
		
		VennData<String> vdata = new VennData<String>(sigSets);
		NumberVennDiagram nvenn = new NumberVennDiagram(vdata);
		nvenn.setLabels(tissues);
		nvenn.setTitle("Metacore GO terms with p <= " + fm.format(p));
		nvenn.saveImage(new File(outDir, "venn_nGO_p_" + fm.format(p) + ".png"), "png");
		
		List<Set<String>> sigSetsMet = new ArrayList<Set<String>>();
		for(String tis : tissues) {
			sigSetsMet.add(extractSignificantTerms(parser.getResult(tis).values(), p, true));
		}
		
		VennData<String> vdataMet = new VennData<String>(sigSetsMet);
		NumberVennDiagram nvennMet = new NumberVennDiagram(vdataMet);
		nvennMet.setLabels(tissues);
		nvennMet.setTitle("Metacore metabolic GO terms with p <= " + fm.format(p));
		nvennMet.saveImage(new File(outDir, "venn_nGO_met_p_" + fm.format(p) + ".png"), "png");
		
		//Save data as text files
		writeSet(new File(outDir, "term_info.txt"), parser, p);
	}
	
	void writeSet(File outFile, GOResultParser results, double p) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		
		out.append("Term Id\tTerm name\tIs metabolic\tSAT p-value\tMAT p-value\tEAT p-value\t" +
				"SAT <= " + p + "\tMAT <= " + p + "\tEAT <= " + p + "\n");
		
		Set<String> allTermNames = new HashSet<String>();
		allTermNames.addAll(results.eat.keySet());
		allTermNames.addAll(results.mat.keySet());
		allTermNames.addAll(results.sat.keySet());
		
		for(String termName : allTermNames) {
			GOResult rEat = results.eat.get(termName);
			GOResult rSat = results.sat.get(termName);
			GOResult rMat = results.mat.get(termName);
			String id = "";
			boolean metabolic = false;
			if(rEat != null) {
				id = rEat.termId;
				metabolic = rEat.metabolic;
			} else if(rSat != null) {
				id = rSat.termId;
				metabolic = rSat.metabolic;
			} else if(rMat != null) {
				id = rMat.termId;
				metabolic = rMat.metabolic;
			}
			out.append(id + "\t");
			out.append(termName + "\t");
			out.append(metabolic + "\t");
			out.append((rSat == null ? "" : rSat.pvalue) + "\t");
			out.append((rMat == null ? "" : rMat.pvalue) + "\t");
			out.append((rEat == null ? "" : rEat.pvalue) + "\t");
			out.append((rSat == null ? false : rSat.pvalue <= p) + "\t");
			out.append((rMat == null ? false : rMat.pvalue <= p) + "\t");
			out.append((rEat == null ? false : rEat.pvalue <= p) + "\t");
			out.append("\n");
		}
		out.close();
	}
	
	Set<String> extractSignificantTerms(Collection<GOResult> results, double p, boolean shouldBeMetabolite) {
		Set<String> sig = new HashSet<String>();
		
		for(GOResult r : results) {
			if(r.pvalue <= p) {
				if(shouldBeMetabolite) {
					if(r.metabolic) sig.add(r.termName);
				} else {
					sig.add(r.termName);
				}
			}
		}
		System.out.println(sig.size());
		return sig;
	}
	
	private static class GOResultParser {
		static final String METABOLISM_TERM = "GO:0008152";
		static final int MAT_COL = 0;
		static final int EAT_COL = 5;
		static final int SAT_COL = 10;
		
		Map<String, GOResult> mat = new HashMap<String, GOResult>();
		Map<String, GOResult> eat = new HashMap<String, GOResult>();
		Map<String, GOResult> sat = new HashMap<String, GOResult>();
		
		GOTree goTree;
		
		public GOResultParser(File resultsFile, GOTree goTree) throws IOException {
			this.goTree = goTree;
			
			BufferedReader in = new BufferedReader(new FileReader(resultsFile));
			
			String line = in.readLine(); //Skip headers
			line = in.readLine();
			
			while((line = in.readLine()) != null) {
				parseLine(line, MAT_COL, mat);
				parseLine(line, EAT_COL, eat);
				parseLine(line, SAT_COL, sat);
			}
			in.close();
		}
		
		public Map<String, GOResult> getResult(String tissue) {
			if("SAT".equals(tissue)) return sat;
			if("EAT".equals(tissue)) return eat;
			if("MAT".equals(tissue)) return mat;
			return null;
		}
		
		private void parseLine(String line, int startCol, Map<String, GOResult> results) {
			int col_term = startCol;
			int col_c = startCol + 1;
			int col_m = startCol + 2;
			int col_p = startCol + 3;
			
			String[] cols = line.split("\t", -1);
			
			if("".equals(cols[col_term])) return; //No result for this line
			
			//Find associated GOTerm
			GOTerm term = null;
			Set<GOTerm> nameTerms = goTree.findTermsByName(cols[col_term]);
			if(nameTerms.size() != 1) System.err.println("No unique GO Term not found for: " + 
					cols[col_term] + " (found " + nameTerms.size() + ")");
			else term = nameTerms.iterator().next();
			
			GOResult result = new GOResult(
				term == null ? "??" : term.getId(),
				cols[col_term],
				Integer.parseInt(cols[col_m]),
				Integer.parseInt(cols[col_c]),
				Double.parseDouble(cols[col_p])
			);
			
			//Find out if is metabolic
			if(term != null) {
				GOTerm met = goTree.getTerm(METABOLISM_TERM);
				result.setMetabolic(goTree.getRecursiveParents(term.getId()).contains(met));
			}
			results.put(result.termName, result);
		}
	}
	
	private static class GOResult {
		String termId;
		String termName;
		
		int measured;
		int changed;
		
		double pvalue;
		
		boolean metabolic;
		
		public GOResult(String termId, String termName, int measured, int changed, double pvalue) {
			this.termId = termId;
			this.termName = termName;
			this.measured = measured;
			this.changed = changed;
			this.pvalue = pvalue;
		}
		
		public void setMetabolic(boolean metabolic) {
			this.metabolic = metabolic;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((termId == null) ? 0 : termId.hashCode());
			result = prime * result
					+ ((termName == null) ? 0 : termName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GOResult other = (GOResult) obj;
			if (termId == null) {
				if (other.termId != null)
					return false;
			} else if (!termId.equals(other.termId))
				return false;
			if (termName == null) {
				if (other.termName != null)
					return false;
			} else if (!termName.equals(other.termName))
				return false;
			return true;
		}
	}
}
