package pps2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.bridgedb.AttributeMapper;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.util.Utils;

import venn.VennData;

public class ConstantsPPS2 {
	private static final File idMapperFile = new File("/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090720.bridge");
	private static IDMapperRdb idMapper;
	private static DataSource ds = BioDataSource.ENTREZ_GENE;
	
	static IDMapperRdb getIdMapper() throws IDMapperException {
		if(idMapper == null) idMapper = SimpleGdbFactory.createInstance("" + idMapperFile, new DataDerby(), 0);
		return idMapper;
	}
	
	static final File pathwayDir = new File("/home/thomas/data/pathways/20091022/Mm");
	static final File goOboFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/gene_ontology_edit.obo");
	static final File goAnnotFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/mart_mm_proc.txt");
	
	static final String[] termNames = new String[] {
			//"GO:0006629", //Lipid
			//"GO:0005975", //Carbohydrate
			//"GO:0006519", //Amino acid
			//"GO:0006139", //Nucleotide
			//"GO:0019538", //Protein
			"GO:0008152", //Metabolic process
			"GO:0006979",
			"GO:0006954"
	};
	
	static GOTerm[] getGOTerms(GOTree tree) {
		GOTerm[] terms = new GOTerm[termNames.length];
		for(int i = 0; i < termNames.length; i++) terms[i] = tree.getTerm(termNames[i]);
		return terms;
	}
	
	static String[] getGOTermLabels(GOTerm[] terms) {
		String[] lbs = new String[terms.length];
		for(int i = 0; i < lbs.length; i++) lbs[i] = terms[i].getName();
		return lbs;
	}
	
	static GOAnnotations<XrefAnnotation> getGOAnnotations(GOTree tree) throws IOException {
		return GOAnnotations.read(
				goAnnotFile, tree, new GOAnnotationFactory<XrefAnnotation>() {
			public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
				Set<XrefAnnotation> annot = new HashSet<XrefAnnotation>();
				try {
					for(Xref x : getIdMapper().mapID(new Xref(id, BioDataSource.ENSEMBL_MOUSE), ds)) {
						annot.add(new XrefAnnotation(x.getId(), x.getDataSource(), evidence));
					}
				} catch(IDMapperException e) {
					Logger.log.error("Unable to map ids for GO annotations", e);
				}
				return annot;
			}
		});
	}
	
	static void saveVennAsExcel(File excelFile, VennData<Xref> vd, String[] labels) throws IOException {
		HSSFWorkbook excel = new HSSFWorkbook();
		HSSFSheet sheet = excel.createSheet();

		//Create headers
		Map<Integer, String> headers = new HashMap<Integer, String>();
		headers.put(vd.getUnionIndex(0), labels[0]);
		headers.put(vd.getUnionIndex(1), labels[1]);
		headers.put(vd.getUnionIndex(0, 1), labels[0] + "-" + labels[1]);
		if(vd.getNrSets() == 3) {
			headers.put(vd.getUnionIndex(2), labels[2]);
			headers.put(vd.getUnionIndex(0, 2), labels[0] + "-" + labels[2]);
			headers.put(vd.getUnionIndex(1, 2), labels[1] + "-" + labels[2]);
			headers.put(vd.getUnionIndex(0, 1, 2), labels[0] + "-" + labels[1] + "-" + labels[2]);
		}
		//Convert sets to lists
		int maxSize = 0;
		Map<Integer, ArrayList<Xref>> setLists = new HashMap<Integer, ArrayList<Xref>>();
		for(int i : vd.getUnionIndices()) {
			SortedSet<Xref> set = new TreeSet<Xref>(vd.getUnion(i));
			setLists.put(i, new ArrayList<Xref>(set));
			maxSize = Math.max(maxSize, set.size());
		}

		//Print headers
		HSSFRow headerRow = sheet.createRow(0);
		short r = 0;
		for(int i : vd.getUnionIndices()) {
			headerRow.createCell(r++).setCellValue(headers.get(i));
		}

		//Print items
		for(int i = 0; i < maxSize; i++) {
			HSSFRow row = sheet.createRow(i+1);
			short c = 0;
			for(int j : vd.getUnionIndices()) {
				HSSFCell cell = row.createCell(c++);
				ArrayList<Xref> l = setLists.get(j);
				if(l.size() > i) {
					Xref x = l.get(i);
					String link = "HYPERLINK(\"" + x.getUrl() + "\",\"" + getSymbol(x) + "\")";
					cell.setCellFormula(link);
				}
			}
		}

		FileOutputStream out = new FileOutputStream(excelFile);
		excel.write(out);
		out.close();
	}
	
	static String getSymbol(Xref x) {
		String symbol = x.getId();
		try {
			Set<String> attr = ((AttributeMapper)getIdMapper()).getAttributes(x, "Symbol");
			if(attr != null && attr.size() > 0) {
				symbol = attr.iterator().next();
				for(String s : attr) {
					if(s.matches("^[A-Z]{1}[a-z]{1}.*")) {
						symbol = s;
						break;
					}
				}
			}
		} catch(IDMapperException e) {
			e.printStackTrace();
		}
		return symbol; 
	}
}
