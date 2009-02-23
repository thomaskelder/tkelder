package org.pathvisio.gsea;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.pathvisio.data.DBConnDerby;
import org.pathvisio.data.DataException;
import org.pathvisio.data.Gdb;
import org.pathvisio.data.Sample;
import org.pathvisio.data.SimpleGdb;
import org.pathvisio.data.SimpleGex;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.Pathway;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.ClsParser;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.parsers.GmxParser;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;

public class ConfigPanel extends JPanel implements ActionListener {
	GexGSEA gsea;
	Dataset dataset;
	List<GeneSet> geneSets;
	Template template;
	
	List<Sample> samples = new ArrayList<Sample>();
	List<Boolean> sampleSelected = new ArrayList<Boolean>();
	List<String> sampleClasses = new ArrayList<String>();
	
	JTextField textGdb, textGex, textPws, textOut;
	JTable tempTable;
	JButton startButton;
	
	public ConfigPanel(Gdb gdb, SimpleGex gex, File pathwayDir) {
		gsea = new GexGSEA(gdb, gex);
		createContents();
		
		if(gdb != null) textGdb.setText(gdb.getDbName());
		if(gex != null) textGex.setText(gex.getDbName());
		
		if(pathwayDir != null) {
			textPws.setText(pathwayDir.getAbsolutePath());
		}
		
		if(gdb != null && gex != null) {
			loadDataset();
		}
		if(pathwayDir != null) {
			loadPathways();
		}

		textOut.setText(new File(".").getAbsolutePath());
		
		refresh();
	}

	TempTableModel tempTableModel;
	
	private void refresh() {
		if(dataset != null && geneSets != null) {
			Logger.log.info("Enabling sample table");
			tempTable.setEnabled(true);
			startButton.setEnabled(true);
			//Refresh the table content
			samples.clear();
			sampleSelected.clear();
			sampleClasses.clear();
			samples.addAll(gsea.getGex().getSamples(Types.REAL));
			for(Sample s : samples) {
				sampleSelected.add(false);
				sampleClasses.add("");
			}
			Logger.log.info(samples + "");
			Logger.log.info(sampleSelected + "");
			Logger.log.info(sampleClasses + "");
		} else {
			Logger.log.info("Disabling sample table");
			tempTable.setEnabled(false);
			startButton.setEnabled(false);
		}
		revalidate();
	}

	private void loadDataset() {
		try {
			if(gsea.canCreateDataset()) {
				dataset = gsea.createDataSet();
			}
		} catch(Exception e) {
			reportError(e);
		}
		refresh();
	}
	
	private void loadPathways() {
		try {
			if(gsea.canCreateGeneSets() && textPws.getText() != null) {
				File pathwayDir = new File(textPws.getText());
				List<Pathway> pathways = new ArrayList<Pathway>();
				GexGSEA.loadPathways(pathwayDir, pathways);
				geneSets = gsea.createGeneSets(pathways);
			}
		} catch(Exception e) {
			reportError(e);
		}
	}
	
	private void doAnalysis() {
		if(dataset == null) reportError(new IllegalArgumentException("No dataset loaded"));
		if(geneSets == null) reportError(new IllegalArgumentException("No pathways loaded"));
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		String datetime = sdf.format(cal.getTime());
		String name = "gsea-" + datetime + ".txt";
		File outputDir = new File(textOut.getText());
		File outputFile = new File(outputDir, name);
		
		List<Sample> selected = new ArrayList<Sample>();
		List<String> classes = new ArrayList<String>();
		for(int i = 0; i < samples.size(); i++) {
			Boolean use = sampleSelected.get(i);
			if(use != null && use.booleanValue()) {
				selected.add(samples.get(i));
				classes.add(sampleClasses.get(i));
			}
		}
		
		Template template = gsea.createTemplate(selected, classes);
		
		try {
			//Write GSEA files
			GctParser gctp = new GctParser();
			gctp.export(dataset, new File(outputDir, "dataset-" + datetime + ".gct"));
			
			GmxParser gmxp = new GmxParser();
			gmxp.export(
				new DefaultGeneSetMatrix("pathways" , geneSets),
				new File(outputDir, "genesets-" + datetime + ".gmx")
			);
			
			ClsParser clsp = new ClsParser();
			clsp.export(
					template,
					new File(outputDir, "template-" + datetime + ".cls")
			);
			
			//Do the analysis
			EnrichmentDb result = gsea.doCalculation(dataset, template, geneSets);
			gsea.writeResultTable(result, new FileWriter(outputFile));
		} catch (Exception e) {
			reportError(e);
		}
	}
	
	private void reportError(Throwable e) {
		String msg = "Error: " + e.getMessage() + " (" + e.getClass() + ")";
		JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
		Logger.log.error(msg, e);
	}
	
	/*
	Gene database: [ read-only text field ] (Browse)
	Expression data: [ read-only text field ] (Browse)
	Pathways: [ read-only text field ] (Browse)
	-------------------------------------------------
	|Sample	(readonly)|Use (checkbox)|Class (string)|
		s1					[ ]				text
		s2					[ ]				text
		...
	-------------------------------------------------
	Output directory: [ text field ] (Browse)

				(Start analysis)
	 */
	private void createContents() {
		FormLayout layout = new FormLayout("pref, 4dlu, fill:pref:grow, 4dlu, pref", "");
		setLayout(layout);
		DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
		
		JButton browseGdb = new JButton("Browse");
		browseGdb.setActionCommand(ACTION_BRS_GDB);
		browseGdb.addActionListener(this);
		JButton browseGex = new JButton("Browse");
		browseGex.setActionCommand(ACTION_BRS_GEX);
		browseGex.addActionListener(this);
		JButton browsePws = new JButton("Browse");
		browsePws.setActionCommand(ACTION_BRS_PWS);
		browsePws.addActionListener(this);
		JButton browseOut = new JButton("Browse");
		browseOut.setActionCommand(ACTION_BRS_OUT);
		browseOut.addActionListener(this);
		startButton = new JButton("Start analysis");
		startButton.setActionCommand(ACTION_START);
		startButton.addActionListener(this);
		
		textGdb = new JTextField();
		textGdb.setEditable(false);
		textGex = new JTextField();
		textGex.setEditable(false);
		textPws = new JTextField();
		textPws.setEditable(false);
		textOut = new JTextField();
		textOut.setEditable(false);
		
		tempTableModel = new TempTableModel();
		tempTable = new JTable(tempTableModel);
		tempTable.setMinimumSize(new Dimension(200, 300));
		tempTable.setDefaultRenderer(Sample.class, new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				if(value == null) value = "";
				else value = ((Sample)value).getName();
				return super.getTableCellRendererComponent(
						table, value, isSelected, hasFocus, row, column
				);
			}
		});

		builder.setDefaultDialogBorder();
		builder.append("Gene database:", textGdb, browseGdb);
		builder.nextLine();
		builder.append("Expression data:", textGex, browseGex);
		builder.nextLine();
		builder.append("Pathways:", textPws, browsePws);
		builder.nextLine();
		builder.append(new JScrollPane(tempTable), 5);
		builder.nextLine();
		builder.append("Output directory:", textOut, browseOut);
		builder.nextLine();
		builder.append(startButton, 5);
	}

	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if(ACTION_BRS_GDB.equals(action)) {
			DBConnDerby dbc = new DBConnDerby();
			dbc.setDbType(DBConnDerby.TYPE_GDB);
			String dbName = dbc.openChooseDbDialog(this);
			if(dbName != null) {
				try {
					Gdb gdb = new SimpleGdb(dbName, dbc, DBConnDerby.PROP_NONE);
					gsea.setGdb(gdb);
					loadDataset();
					loadPathways();
					textGdb.setText(dbName);
				} catch (DataException e1) {
					reportError(e1);
				}
			}
		} else if (ACTION_BRS_GEX.equals(action)) {
			DBConnDerby dbc = new DBConnDerby();
			dbc.setDbType(DBConnDerby.TYPE_GEX);
			String dbName = dbc.openChooseDbDialog(this);
			if(dbName != null) {
				try {
					SimpleGex gex = new SimpleGex(dbName, false, dbc);
					gsea.setGex(gex);
					loadDataset();
					textGex.setText(dbName);
				} catch (DataException e1) {
					reportError(e1);
				}
			}
		} else if (ACTION_BRS_PWS.equals(action)) {
			JFileChooser fc = new JFileChooser(textPws.getText());
			fc.setDialogTitle("Select pathway directory");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int exit = fc.showOpenDialog(this);
			File f = fc.getSelectedFile();
			if(exit == JFileChooser.APPROVE_OPTION && f != null) {
				textPws.setText(f.getAbsolutePath());
				loadPathways();
			}
		} else if (ACTION_BRS_OUT.equals(action)) {
			JFileChooser fc = new JFileChooser(textOut.getText());
			fc.setDialogTitle("Select output directory");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int exit = fc.showOpenDialog(this);
			File f = fc.getSelectedFile();
			if(exit == JFileChooser.APPROVE_OPTION && f != null) {
				textOut.setText(f.getAbsolutePath());
			}
		} else if (ACTION_START.equals(action)) {
			doAnalysis();
		}
	}
	
	static final String ACTION_BRS_GDB = "gdb";
	static final String ACTION_BRS_GEX = "gex";
	static final String ACTION_BRS_PWS = "pws";
	static final String ACTION_BRS_OUT = "out";
	static final String ACTION_START = "start";
	
	private class TempTableModel implements TableModel {
		public int getColumnCount() {
			return 3;
		}
		public int getRowCount() {
			return samples.size();
		}
		public Object getValueAt(int row, int column) {
			switch(column) {
			case 0:
				return samples.get(row);
			case 1:
				return sampleSelected.get(row);
			case 2:
				return sampleClasses.get(row);
			default:
				return null;	
			}
		}
		
		public void setValueAt(Object value, int row, int column) {
			switch(column) {
			case 0:
				samples.set(row, (Sample)value);
				break;
			case 1:
				sampleSelected.set(row, (Boolean)value);
				break;
			case 2:
				sampleClasses.set(row, (String)value);
			}
		}
		
		public void addTableModelListener(TableModelListener l) {
			
		}
		
		public Class<?> getColumnClass(int columnIndex) {
			switch(columnIndex) {
			case 0:
				return Sample.class;
			case 1:
				return Boolean.class;
			case 2:
				return String.class;
			default:
				return null;
			}
		}
		
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
			case 0:
				return "Sample";
			case 1:
				return "Select";
			case 2:
				return "Class";
			default:
				return null;
			}
		}
		
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if(columnIndex == 0) return false;
			return true;
		}
		
		public void removeTableModelListener(TableModelListener l) {
			
		}
	}
}
