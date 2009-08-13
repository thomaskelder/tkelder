package org.wikipathways.stats;

import org.wikipathways.stats.db.WPDatabase;

public interface Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException;
}
