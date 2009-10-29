package org.apa.tools;

import java.io.File;
import java.util.List;

import org.apa.AtlasException;
import org.apa.AtlasSessionUtils;
import org.apa.analysis.AnalysisMethod;
import org.apa.analysis.MeanTAnalysis;
import org.apa.analysis.ZScoreAnalysis;
import org.apa.data.Statistic;
import org.apa.report.AnalysisMatrix;
import org.apa.report.CompareAnalysisMethods;
import org.apa.report.EnrichedPathwayFrequencies;
import org.apa.report.ExpressionPlots;
import org.apa.report.CompareAnalysisMethods.Transform;
import org.apa.report.EnrichedPathwayFrequencies.IsSignificant;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

public class GlobalAnalysis {
	public static void main(String[] args) {
		try {
			GlobalAnalysis analysis = new GlobalAnalysis();
			
			//analysis.doEnrichmentFrequencies();
			//analysis.doZscoreFrequencies();
			//analysis.compareAnalysisMethods();
			//analysis.expression();
			analysis.analysisMatrix();
			
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	File sessionConfig = new File("hibernate-example.cfg.xml");
	File outPath = new File("global-analysis");
	
	SessionFactory sessionFactory;
	public GlobalAnalysis() {
		outPath.mkdirs();
		sessionFactory = AtlasSessionUtils.createSessionFactory(sessionConfig);
	}
	
	void analysisMatrix() throws AtlasException {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			AnalysisMatrix mtrx = new AnalysisMatrix(session, "Homo sapiens");
			mtrx.saveReport(new File(outPath, "analysis_matrix"), "Hs");
			
			mtrx = new AnalysisMatrix(session, "Mus musculus");
			mtrx.saveReport(new File(outPath, "analysis_matrix"), "Mm");
			
			mtrx = new AnalysisMatrix(session, "Rattus norvegicus");
			mtrx.saveReport(new File(outPath, "analysis_matrix"), "Rn");
		} finally {
			session.close();
		}
	}
	
	void expression() throws AtlasException {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			ExpressionPlots plots = new ExpressionPlots(session);
			plots.saveReport(new File(outPath, "expression"), "");
		} finally {
			session.close();
		}
	}
	
	void compareAnalysisMethods() throws AtlasException {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			//Z-score p-value vs enrichment p-value
			CompareAnalysisMethods compare = new CompareAnalysisMethods(
					session, MeanTAnalysis.TYPE, ZScoreAnalysis.TYPE, 
					AnalysisMethod.VALUE_PVALUE, AnalysisMethod.VALUE_PVALUE);
			compare.setTrans1(new Transform() {
				public String getLabel() {
					return "-log10(p-value) - enrichment";
				}
				public double transform(double d) {
					d = -Math.log10(d);
					if(Double.isInfinite(d)) d = 4;
					return d;
				}
			});
			compare.setTrans2(new Transform() {
				public String getLabel() {
					return "-log10(p-value) - z-score";
				}
				public double transform(double d) {
					d = -Math.log10(d);
					if(Double.isInfinite(d)) d = 4;
					return d;
				}
			});
			compare.saveReport(new File(outPath, "compare_methods"), "enrichment_vs_zscore_pvalue");
			
			//Z-score vs enrichment score
			compare = new CompareAnalysisMethods(
					session, MeanTAnalysis.TYPE, ZScoreAnalysis.TYPE, 
					AnalysisMethod.VALUE_SCORE, AnalysisMethod.VALUE_SCORE);
			compare.saveReport(new File(outPath, "compare_methods"), "enrichment_vs_zscore");
			
			
			compare = new CompareAnalysisMethods(
					session, ZScoreAnalysis.TYPE, ZScoreAnalysis.TYPE, 
					AnalysisMethod.VALUE_PVALUE, AnalysisMethod.VALUE_SCORE);
			compare.setTrans1(new Transform() {
				public String getLabel() {
					return "-log10(p-value)";
				}
				public double transform(double d) {
					d = -Math.log10(d);
					if(Double.isInfinite(d)) d = 4;
					return d;
				}
			});
			compare.saveReport(new File(outPath, "compare_methods"), "zscore_vs_pvalue");
		} finally {
			session.close();
		}
	}
	
	void doEnrichmentFrequencies() throws AtlasException {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			List<String> organisms = (List<String>)session.createQuery(
				"select distinct organism from Experiment"	
			).list();
			
			String type = "enrichment_squaredT_mean";
			final double maxP = 0.001;
			IsSignificant sig = new IsSignificant() {
				public boolean isSignificant(Statistic stat) {
					double p = Double.parseDouble(stat.getValue("p-value"));
					return !Double.isNaN(p) && p <= maxP;
				}
			};
			for(String org : organisms) {
				EnrichedPathwayFrequencies frq = new EnrichedPathwayFrequencies(
						session, type, org, sig
				);
				frq.saveReport(new File(outPath, "enrichment_frequencies"), org);
			}
		} finally {
			session.close();
		}
	}
	
	void doZscoreFrequencies() throws AtlasException {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		try {
			List<String> organisms = (List<String>)session.createQuery(
				"select distinct organism from Experiment"	
			).list();
			
			String type = ZScoreAnalysis.TYPE;
			
			final double minZ = 2;
			IsSignificant sig = new IsSignificant() {
				public boolean isSignificant(Statistic stat) {
					double p = Double.parseDouble(stat.getValue("z-score"));
					return !Double.isNaN(p) && p >= minZ;
				}
			};
			for(String org : organisms) {
				EnrichedPathwayFrequencies frq = new EnrichedPathwayFrequencies(
						session, type, org, sig
				);
				frq.saveReport(new File(outPath, "zscore_frequencies"), org);
			}
		} finally {
			session.close();
		}
	}
}
