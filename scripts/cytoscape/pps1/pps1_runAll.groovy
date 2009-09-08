/*
/home/thomas/code/googlerepo/scripts/cytoscape/pps1/pps1_runAll.groovy
*/
import cytoscape.*;
import cytoscape.actions.*;
import cytoscape.task.*;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import walkietalkie.*;

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
    static String dataPath = "/home/thomas/projects/pps1/stat_results/";
    static String outPath = "/home/thomas/projects/pps1/path_results/bigcat/network/revised/";
    static String expression = "[anova_qvalue_TIS] <= 0.05";
    static String[] visAttrTmp = ["TIS_relt0_1", "TIS_relt0_6", "TIS_relt0_9", "TIS_relt0_12"];
    static String[] tissues = ["Liver", "Muscle", "WAT"];
    static String idmPath = "/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20090509.pgdb";
    static File dataFile = new File("/home/thomas/projects/pps1/stat_results/pps1_expr_anova.pgex");
    
    static String pathwayPath = "/home/thomas/data/pathways/20090715";
}

GroovyShell gsh = new GroovyShell(this.class.classLoader, getBinding());
String scriptPath = "/home/thomas/code/googlerepo/scripts/cytoscape/pps1/";
boolean forceCalculate = false;
new File(PPSGlobals.outPath).mkdirs();
WalkieTalkiePlugin.registerVisualStyle();

File networkSession = new File(PPSGlobals.outPath, "pps1_network.cys");
File graphSession = new File(PPSGlobals.outPath, "pps1_vis.cys");

if(forceCalculate || !networkSession.exists()) {
    gsh.evaluate(new File(scriptPath, "pps1_loadNetworks.groovy"));
    saveSession(networkSession);
} else {
    if(!graphSession.exists())loadSession(networkSession);
}

if(forceCalculate || !graphSession.exists()) {
	gsh.evaluate(new File(scriptPath, "pps1_filtered.groovy"));
	saveSession(graphSession);
} else {
    loadSession(graphSession);
}

gsh.evaluate(new File(scriptPath, "pps1_applyGraphics.groovy"));