package org.wikipathways.stats.taskimpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

/**
 * Find out about newly created pathways within a given time interval
 * @author thomas
 */
public class NewPathways implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Calendar cal = Calendar.getInstance();
			cal.clear();
		    cal.set(Calendar.YEAR, 2009);
		    cal.set(Calendar.MONTH, 11);
		    cal.set(Calendar.DATE, 1);
		    cal.set(Calendar.HOUR_OF_DAY, 1);
		    
			Date start = cal.getTime();
			
//			cal.clear();
//		    cal.set(Calendar.YEAR, 2010);
//		    cal.set(Calendar.MONTH, 1);
//		    cal.set(Calendar.DATE, 13);
//		    cal.set(Calendar.HOUR_OF_DAY, 10);
//			Date end = cal.getTime();
			Date end = new Date();
			
			System.out.println(start);
			System.out.println(end);
			
			Set<PathwayInfo> pathways = PathwayInfo.getSnapshot(db, end);
			List<NewInfo> info = new ArrayList<NewInfo>();
			
			for(PathwayInfo p : pathways) {
				int rev = p.getFirstRevision();
				//Exclude private pathways
				if(p.isPrivate()) continue;
				
				Date created = p.getRevisionTime(rev);
				if(created.after(start) && created.before(end)) {
					int uid = p.getRevisionUser(rev);
					User user = User.fromDb(db, uid);
					//Only include pathways created by real users
					if(!"MaintBot".equals(user.getName())) {
						NewInfo i = new NewInfo();
						i.created = created;
						i.species = p.getSpecies();
						i.title = p.getTitle();
						i.user = user.getFullNameSave();
						i.id = p.getPathwayId();
						info.add(i);
					}
				}
			}
			
			Collections.sort(info, new Comparator<NewInfo>() {
				public int compare(NewInfo a, NewInfo b) {
					if(a.created.equals(b.created)) return 0; 
					if(a.created.before(b.created)) return -1;
					return 1;
				}
			});
			
			for(NewInfo i : info) {
				System.out.println(i.title + " (" + i.species + "), http://www.wikipathways.org/index.php/Pathway:" + i.id + ", created by " + i.user + " at " + i.created);
			}
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
	
	private class NewInfo {
		String species;
		String title;
		String user;
		String id;
		Date created;
	}
}
