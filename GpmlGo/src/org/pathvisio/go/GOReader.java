package org.pathvisio.go;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GOReader {
	public static GOTree readGOTree(File ontFile) throws IOException {
		Set<GOTerm> terms = new HashSet<GOTerm>();
		
		BufferedReader in = new BufferedReader(new FileReader(ontFile));
		String line;
		while((line = in.readLine()) != null) {
			if(line.startsWith(TERM)) {
				//Next line is id
				String id = in.readLine();
				id = id.replaceFirst(ID_PREFIX, "");
				//Next line is name
				String name = in.readLine();
				name = name.replaceFirst(NAME_PREFIX, "");
				
				GOTerm term = new GOTerm(id, name);
				
				//Continue reading until next empty line
				while((line = in.readLine()) != null && line.length() > 0) {
					parsePropertyLine(line, term);
				}
				
				if(!term.isObsolete()) terms.add(term); //Don't add obsolete terms
			}
		}
		return new GOTree(terms);
	}
	
	private static void parsePropertyLine(String line, GOTerm term) {
		if(line.startsWith(IS_A_PREFIX)) {
			String parent = line.replaceFirst(IS_A_PREFIX, "");
			Matcher match = GO_ID_PATTERN.matcher(parent);
			if(match.find()) {
				parent = match.group();
			}
			term.addParent(parent);
		} else if(line.startsWith(IS_OBSOLETE_PREFIX)) {
			term.setObsolete(true);
		} else if(line.startsWith(SYNONYM_PREFIX)) {
			Matcher match = SYNONYM_EXTRACT_PATTERN.matcher(line);
			if(match.find()) {
				String syn = match.group(1);
				term.addSynonym(syn);
			}
		}
	}
	
	private static final Pattern SYNONYM_EXTRACT_PATTERN = Pattern.compile("synonym: \"(.+)\"");
	private static final Pattern GO_ID_PATTERN = Pattern.compile("GO:[0-9]{7}");
	private static final String TERM = "[Term]";
	private static final String IS_A_PREFIX = "is_a: ";
	private static final String IS_OBSOLETE_PREFIX= "is_obsolete: ";
	private static final String ID_PREFIX = "id: ";
	private static final String NAME_PREFIX = "name: ";
	private static final String SYNONYM_PREFIX = "synonym: ";
}
