package org.wikipathways.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.wikipathways.stats.taskimpl.CurationTagCounts;
import org.wikipathways.stats.taskimpl.PathwayCounts;
import org.wikipathways.stats.taskimpl.PathwayCountsBySpecies;

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
