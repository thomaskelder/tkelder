package org.wikipathways.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.wikipathways.stats.taskimpl.CurationEvent;
import org.wikipathways.stats.taskimpl.CurationTagCounts;
import org.wikipathways.stats.taskimpl.EditCounts;
import org.wikipathways.stats.taskimpl.EditFrequencies;
import org.wikipathways.stats.taskimpl.GeneralReport;
import org.wikipathways.stats.taskimpl.NewPathways;
import org.wikipathways.stats.taskimpl.PathwayCounts;
import org.wikipathways.stats.taskimpl.PathwayCountsBySpecies;
import org.wikipathways.stats.taskimpl.UserCounts;
import org.wikipathways.stats.taskimpl.WebserviceActivity;
import org.wikipathways.stats.taskimpl.XrefCoverage;

/**
 * Registry for all available tasks
 * @author thomas
 */
public class TaskRegistry {
	Map<String, Task> tasks = new HashMap<String, Task>();
	
	public TaskRegistry() {
		init();
	}
	
	public void init() {
		//Register default tasks
		registerTask(PathwayCounts.class.getName(), new PathwayCounts());
		registerTask(PathwayCountsBySpecies.class.getName(), new PathwayCountsBySpecies());
		registerTask(CurationTagCounts.class.getName(), new CurationTagCounts());
		registerTask(XrefCoverage.class.getName(), new XrefCoverage());
		registerTask(UserCounts.class.getName(), new UserCounts());
		registerTask(EditFrequencies.class.getName(), new EditFrequencies());
		registerTask(CurationEvent.class.getName(), new CurationEvent());
		registerTask(NewPathways.class.getName(), new NewPathways());
		registerTask(WebserviceActivity.class.getName(), new WebserviceActivity());
		registerTask(GeneralReport.class.getName(), new GeneralReport());
		registerTask(EditCounts.class.getName(), new EditCounts());
	}
	
	public Task getTask(String id) {
		return tasks.get(id);
	}
	
	public void registerTask(String id, Task task) {
		tasks.put(id, task);
	}
	
	public Collection<Task> getAllTasks() {
		return tasks.values();
	}
	
	public static final String TASK_PATHWAY_COUNT = "pathway_count";
}
