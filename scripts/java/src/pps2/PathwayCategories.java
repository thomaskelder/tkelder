package pps2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pathvisio.util.FileUtils;

/**
 * Utility to parse Susan's categorized list of pathways and 
 * assign annotations to a set of pathway files.
 * @author thomas
s */
public class PathwayCategories {
	Map<File, Category> categories = new HashMap<File, Category>();
	
	public static PathwayCategories parse(File categoryFile, File pathwayDir) throws IOException {
		List<File> pathwayFiles = FileUtils.getFiles(pathwayDir, true);
		
		PathwayCategories cats = new PathwayCategories();
		
		BufferedReader in = new BufferedReader(new FileReader(categoryFile));
		String line = in.readLine(); //Header
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", -1);
			String name = cols[0];
			String url = cols[1];
			String catSymbol = cols[3];
			Category cat = Category.bySymbol.get(catSymbol);
			File f = findPathway(pathwayFiles, name, url);
			if(f != null && cat != null) {
				cats.setCategory(f, cat);
			}
		}
		
		return cats;
	}
	
	static final String WP_URL = "http://www.wikipathways.org/index.php/Pathway:";
	
	static File findPathway(Collection<File> pathwayFiles, String name, String url) {
		for(File f : pathwayFiles) {
			if(url.startsWith(WP_URL)) {
				//Use pathway id
				String id = url.replace(WP_URL, "");
				if(f.getName().contains("_" + id + "_")) {
					return f;
				}
			} else { //Assume Kegg file name structure (org_name)
				if(f.getName().contains(name)) {
					return f;
				}
			}
		}
		
		return null;
	}
	
	public void setCategory(File file, Category cat) {
		categories.put(file, cat);
	}
	
	public Category getCategory(File file) {
		return categories.get(file);
	}
	
	public Collection<File> getCategorizedPathways() {
		return categories.keySet();
	}
	
	public String toString() {
		StringBuilder strb = new StringBuilder();
		for(File f : categories.keySet()) {
			strb.append(f + "\t" + categories.get(f) + "\n");
		}
		return strb.toString();
	}
	
	public enum Category {
		METABOLISM("m", "Metabolism"),
		INFLAMMATION("i", "Inflammation"),
		OXIDATIVE_STRESS("o", "Oxidative stress");
		
		String symbol;
		String name;
		
		private Category(String symbol, String name) {
			this.symbol = symbol;
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		static Map<String, Category> bySymbol = new HashMap<String, Category>();
		static {
			for(Category c : Category.values()) {
				bySymbol.put(c.symbol, c);
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			File categoryFile = new File("/home/thomas/projects/pps2/path_results/bigcat/categories/Pathways_catergorizatio_GO.txt");
			File pathwayDir = new File("/home/thomas/data/pathways/20091001");
			PathwayCategories cats = parse(categoryFile, pathwayDir);
			System.out.println(cats);
			System.out.println(cats.categories.size());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
