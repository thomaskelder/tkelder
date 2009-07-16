/*
/home/thomas/code/googlerepo/scripts/cytoscape/pps3/pps3_runAll.groovy
*/
import cytoscape.*;
import cytoscape.actions.*;
import cytoscape.task.*;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

//Globals
class PPSGlobals {
	static String dataPath = "/home/thomas/projects/pps3/";
	static String outPath = PPSGlobals.dataPath + "network/";
	
	static List<String> stats = [
		"overall",
		"diet",
		"leptin",
		"time"
	];

	static List<String> dataAttr = [
		"10% 1W V", "10% 1W L", "10% 4W V", "10% 4W L", 
		"45% 1W V", "45% 1W L", "45% 4W V", "45% 4W L"   
	];
	
	public static List<String> getVisAttr(String stat) {
	    List<String> visAttr = null;
	    switch(stat) {
	    	case "overall":
	    		visAttr = dataAttr;
	    		break;
	    	case "time":
	    	    visAttr = [ "10% L log2(4W/1W)", "10% V log2(4W/1W)", 
	    	    					"45% L log2(4W/1W)", "45% V log2(4W/1W)" ];
	    		break;
	    	case "leptin":
	    		visAttr = [ "10% 1W log2(V/L)", "10% 4W log2(V/L)", 
	    							"45% 1W log2(V/L)", "45% 4W log2(V/L)" ];
	    		break;
	    	case "diet":
	    		visAttr = [ "1W V log2(45/10%)", "4W V log2(45/10%)", 
	    							"1W L log2(45/10%)", "4W L log2(45/10%)"];
		    	break;
	    }
	    return visAttr;
	}
}

GroovyShell gsh = new GroovyShell(this.class.classLoader, getBinding());

boolean forceCalculate = false;

new File(PPSGlobals.outPath).mkdirs();

String scriptPath = "/home/thomas/code/googlerepo/scripts/cytoscape/pps3/";

File sessionFile = new File(PPSGlobals.outPath, "pps3.cys");
if(forceCalculate || !sessionFile.exists()) {
	gsh.evaluate(new File(scriptPath, "pps3_loadNetworks.groovy"));
	//save the session so we don't have to recalculate each time
	SaveSessionTask task =  new SaveSessionTask(sessionFile.getAbsolutePath());

	JTaskConfig jTaskConfig = new JTaskConfig();
	jTaskConfig.displayCancelButton(false);
	jTaskConfig.setOwner(Cytoscape.getDesktop());
	jTaskConfig.displayCloseButton(true);
	jTaskConfig.displayStatus(true);
	jTaskConfig.setAutoDispose(true);

	TaskManager.executeTask(task, jTaskConfig);
} else {
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

gsh.evaluate(new File(scriptPath, "pps3_applyGraphics.groovy"));
gsh.evaluate(new File(scriptPath, "pps3_exportPng.groovy"));



