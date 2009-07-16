import cytoscape.*;
import cytoscape.view.*;
import cytoscape.actions.*;
import cytoscape.layout.*;
import cytoscape.task.*;
import cytoscape.task.ui.*;
import cytoscape.task.util.*;

String path = "/home/thomas/projects/pps1/path_results/pathvisio-z/network/";
String dpath = "/home/thomas/projects/pps1/stat_results/cytoscape/";

//Load main network
String mainNetwork = path + "mc1_all.sif";
Cytoscape.createNetworkFromFile(mainNetwork, false);

//Load attributes
File[] attrFiles = [ 
    new File(path + "attrType.txt"), 
    new File(path + "attrLabel.txt"),
   new File(dpath + "cy_Liver_relt0_1.txt"),
    new File(dpath + "cy_Liver_relt0_6.txt"),
    new File(dpath + "cy_Liver_relt0_9.txt"),
    new File(dpath + "cy_Liver_relt0_12.txt"),
    new File(dpath + "cy_Muscle_relt0_1.txt"),
    new File(dpath + "cy_Muscle_relt0_6.txt"),
    new File(dpath + "cy_Muscle_relt0_9.txt"),
    new File(dpath + "cy_Muscle_relt0_12.txt"),
    new File(dpath + "cy_WAT_relt0_1.txt"),
    new File(dpath + "cy_WAT_relt0_6.txt"),
    new File(dpath + "cy_WAT_relt0_9.txt"),
    new File(dpath + "cy_WAT_relt0_12.txt"),
    new File(dpath + "cy_zscore_Liver_anova_q0.01.txt"),
    new File(dpath + "cy_zscore_Muscle_anova_q0.01.txt"),
    new File(dpath + "cy_zscore_WAT_anova_q0.01.txt")
];
ImportAttributesTask task = new ImportAttributesTask(attrFiles, ImportAttributesTask.NODE_ATTRIBUTES);
JTaskConfig jTaskConfig = new JTaskConfig();
jTaskConfig.setOwner(Cytoscape.getDesktop());
jTaskConfig.displayCloseButton(true);
jTaskConfig.displayStatus(true);
jTaskConfig.setAutoDispose(true);
TaskManager.executeTask(task, jTaskConfig);

//Load other networks and layout
String[] loadNetworks = [
    "anov_q0.01_WAT.sif",
    "anov_q0.01_Liver.sif",
    "anov_q0.01_Muscle.sif"
];

for(String f in loadNetworks) {
    CyNetwork n = Cytoscape.createNetworkFromFile(path + f, true);
    CyNetworkView view = Cytoscape.getNetworkView(n.getIdentifier());
    CyLayouts.getLayout("kamada-kawai").doLayout(view);
}