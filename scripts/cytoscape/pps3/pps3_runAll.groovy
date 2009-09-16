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
    //Minimum p-value
    static double pvalue = 0.05;
    
    static String dataPath = "/home/thomas/projects/pps3/";
    static String outPath = PPSGlobals.dataPath + "network-" + pvalue + "/";
    static String pathwayPath = "/home/thomas/data/pathways/20090907/mmu";
    
    //Comparisons
    static List<String> stats = [
        "diet",
        "leptin",
        "time"
    ];

    //Column containing the p-value
    static Map<String, String> pcols = new HashMap<String, String>();
    
    static {
        pcols.put(stats[0], "Diet");
        pcols.put(stats[1], "Comp");
        pcols.put(stats[2], "Time");
    }
    
    static List<String> dataAttr = [
        "10% 1W Vehicle", "10% 1W Leptin", "10% 4W Vehicle", "10% 4W Leptin", 
        "45% 1W Vehicle", "45% 1W Leptin", "45% 4W Vehicle", "45% 4W Leptin"   
    ];
    
    public static List<String> getVisAttr(String stat) {
        List<String> visAttr = null;
        switch(stat) {
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

GroovyShell gsh = new GroovyShell(this.class.classLoader, getBinding());

boolean forceCalculate = false;

new File(PPSGlobals.outPath).mkdirs();

String scriptPath = "/home/thomas/code/googlerepo/scripts/cytoscape/pps3/";

File sessionFile = new File(PPSGlobals.outPath, "pps3.cys");
if(forceCalculate || !sessionFile.exists()) {
    gsh.evaluate(new File(scriptPath, "pps3_loadNetworks.groovy"));
    gsh.evaluate(new File(scriptPath, "pps3_applyGraphics.groovy"));
    gsh.evaluate(new File(scriptPath, "pps3_exportPng.groovy"));
    saveSession(sessionFile);
} else {
    loadSession(sessionFile);
}