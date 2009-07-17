/*
/home/thomas/code/googlerepo/scripts/cytoscape/pps3/pps3_runAll.groovy
*/
import cytoscape.*;
import cytoscape.actions.*;
import cytoscape.task.*;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

//Functions
def saveSession = { sessionFile ->
    //save the session
    SaveSessionTask task =  new SaveSessionTask(sessionFile.getAbsolutePath());

    JTaskConfig jTaskConfig = new JTaskConfig();
    jTaskConfig.displayCancelButton(false);
    jTaskConfig.setOwner(Cytoscape.getDesktop());
    jTaskConfig.displayCloseButton(true);
    jTaskConfig.displayStatus(true);
    jTaskConfig.setAutoDispose(true);

    TaskManager.executeTask(task, jTaskConfig);
}
def loadSession = { sessionFile ->
	//load from session file
	OpenSessionTask task = new OpenSessionTask(sessionFile.getAbsolutePath());
	
	Cytoscape.createNewSession();
	
	JTaskConfig jTaskConfig = new JTaskConfig();
	jTaskConfig.displayCancelButton(false);
	jTaskConfig.setOwner(Cytoscape.getDesktop());
	jTaskConfig.displayCloseButton(true);
	jTaskConfig.displayStatus(true);
	jTaskConfig.setAutoDispose(true);
	
	TaskManager.executeTask(task, jTaskConfig);
}

//Globals
class PPSGlobals {
    static String dataPath = "/home/thomas/projects/pps2/stat_results/";
    static String outPath = "/home/thomas/projects/pps2/path_results/bigcat/network";
}

GroovyShell gsh = new GroovyShell(this.class.classLoader, getBinding());

boolean forceCalculate = false;

new File(PPSGlobals.outPath).mkdirs();

String scriptPath = "/home/thomas/code/googlerepo/scripts/cytoscape/pps2/";

File networkSession = new File(PPSGlobals.outPath, "pps2_HFvsLF_t0_network.cys");
File graphSession = new File(PPSGlobals.outPath, "pps2_HFvsLF_t0_vis.cys");

if(forceCalculate || !networkSession.exists()) {
    gsh.evaluate(new File(scriptPath, "pps2_loadNetworks.groovy"));
    saveSession(networkSession);
} else {
	if(!graphSession.exists())loadSession(networkSession);
}

if(forceCalculate || !graphSession.exists()) {
	gsh.evaluate(new File(scriptPath, "pps2_applyGraphics.groovy"));
    saveSession(graphSession);
} else {
	loadSession(graphSession);
}
gsh.evaluate(new File(scriptPath, "pps2_exportPng.groovy"));