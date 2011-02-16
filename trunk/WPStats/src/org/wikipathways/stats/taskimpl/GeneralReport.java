package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.db.CurationTag;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Textual report containing some current statistics for WikiPathways.
 */
public class GeneralReport implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date now = new Date();
			PrintWriter out = new PrintWriter(
					new File(par.getFile(OUT_PATH), "stats_report.txt"));

			//Number of pathways
			Set<PathwayInfo> pwi = PathwayInfo.getSnapshot(db, now);
			Collection<CurationTag> tags = CurationTag.getLatest(db);
			Multimap<String, Integer> tag2pid = new HashMultimap<String, Integer>();
			for(CurationTag t : tags) {
				tag2pid.put(t.getType(), t.getPageId());
			}
			
			int nrPublic = 0;
			int nrFeatured = 0;
			int nrAnalysis = 0;
			int nrPrivate = 0;
			
			for(PathwayInfo p : pwi) {
				if(tag2pid.get("Tutorial").contains(p.getPageId())) continue;
				
				if(p.isPrivate()) nrPrivate++;
				else nrPublic++;
				
				if(tag2pid.get("FeaturedPathway").contains(p.getPageId())) nrFeatured++;
				if(tag2pid.get("AnalysisCollection").contains(p.getPageId())) nrAnalysis++;
			}
			
			out.println("== Number of pathways ==");
			out.println("Number of public pathways (excluding test pathways):\t" + nrPublic);
			out.println("Number of private pathways:\t" + nrPrivate);
			out.println("Number of analysis collection pathways:\t" + nrAnalysis);
			out.println("Number of featured pathways:\t" + nrFeatured);
			
			//Number of users / edits
			Collection<User> users = User.getSnapshot(db, now);
			
			int nrEdited = 0;
			int nrEditedNoTest = 0;
			int nrInactive = 0;
			int editCounts = 0;
			int editCountsPlusBots = 0;
			for(User u : users) {
				//int e = u.getEditCount(db);
				List<Integer> ep = u.getEditPages(db);
				int e = ep.size();
				editCountsPlusBots += e;
				if(u.getName().equals("MaintBot")) continue;
				
				if(e > 0) nrEdited++;
				else nrInactive++;
				editCounts += e;
				
				ep.removeAll(tag2pid.get("Tutorial"));
				if(ep.size() > 0) nrEditedNoTest++;
			}
			
			
			int editCountsValid = 0;
			int editCountsValidPlusBots = 0;
			//Number of edits on still existing pathways which are not test/tutorial pathways
			for(PathwayInfo p : pwi) {
				if(tag2pid.get("Tutorial").contains(p.getPageId())) continue;
				editCountsValid += p.getNrRevisions(db, false);
				editCountsValidPlusBots += p.getNrRevisions(db, true);
			}
			
			out.println();
			out.println("== Number of users ==");
			out.println("Users with at least one pathway edit:\t" + nrEdited);
			out.println("Users with at least one pathway edit (excluding test pathways):\t" + nrEditedNoTest);
			out.println("Inactive users:\t" + nrInactive);
			
			out.println();
			out.println("== Number of edits (excluding bots) ==");
			out.println("Total pathway edit counts:\t" + editCounts);
			out.println("Edit counts (excluding deleted or test pathways):\t" + editCountsValid);
			
			out.println();
			out.println("== Number of edits (including bots) ==");
			out.println("Total pathway edit counts:\t" + editCountsPlusBots);
			out.println("Edit counts (excluding deleted or test pathways):\t" + editCountsValidPlusBots);
			
			out.close();
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}

}
