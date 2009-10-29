package org.apa.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apa.AtlasException;
import org.apa.AtlasSessionUtils;
import org.apa.analysis.AnalysisMethod;
import org.apa.data.Experiment;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Factor;
import org.apa.data.Pathway;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Perform pathway analysis on the experiments and pathways
 * in a hibernate session and store the results in the session.
 * @author thomas
 */
public class SessionAnalysis {
	static final Logger log = Logger.getLogger("org.apa.tools");
	
	@Option(name = "-sessionConfig", required = true, usage = "Session configuration xml file.")
	File sessionConfig;
	
	@Option(name = "-method", required = true, usage = "Full class name(s) of the AnalysisMethod implementation(s) to run.")
	List<String> methods;

	@Option(name = "-reset", required = true, usage = "Remove existing analyses for the given method(s) before running the analysis.")
	boolean reset;
	
	public static void main(String[] args) {
		SessionAnalysis main = new SessionAnalysis();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		SessionFactory sessionFactory = null;
		
		try {
			log.info("Creating analysis method instances");
			List<AnalysisMethod> methods = new ArrayList<AnalysisMethod>();
			for(String mn : main.methods) {
				Class<?> mc = Class.forName(mn);
				AnalysisMethod mi = (AnalysisMethod)mc.getConstructor().newInstance();
				methods.add(mi);
			}
			
			log.info("Creating hibernate session");
			sessionFactory = AtlasSessionUtils.createSessionFactory(main.sessionConfig);
			sessionFactory.getCurrentSession().beginTransaction();

			if(main.reset) {
				Session session = sessionFactory.openSession();
				Transaction tx = session.beginTransaction();
				
				for(AnalysisMethod m : methods) {
					log.info("Removing existing analyses for " + m);
					Collection<ExperimentAnalysis> analyses = AtlasSessionUtils.getAnalysesByType(session, m.getType());
					for(ExperimentAnalysis ea : analyses) {
						session.delete(ea);
					}
				}
				
				tx.commit();
			}
			
			log.info("Building pathway map");
			Multimap<String, Pathway> setsByOrganism = new HashMultimap<String, Pathway>();
			for(String id : AtlasSessionUtils.getPathways(sessionFactory.getCurrentSession())) {
				Pathway p = (Pathway)sessionFactory.getCurrentSession().get(Pathway.class, id);
				setsByOrganism.put(p.getOrganism(), p);
			}

			log.info("Starting analyses");
			Collection<String> experiments = AtlasSessionUtils.getExperiments(sessionFactory.getCurrentSession());
			int i = 0;
			for(String expId : experiments) {
				log.info("Processing experiment " + expId + "(" + i++ + " out of " + experiments.size() + ")");
				Experiment exp = (Experiment)sessionFactory.getCurrentSession().get(Experiment.class, expId);
				Collection<Pathway> pathways = setsByOrganism.get(exp.getOrganism());
				if(pathways != null) {
					performAnalyses(methods, sessionFactory, exp, pathways);
				}
			}
		} catch(Exception e) {
			log.warning("Error performing analysis: " + e.getMessage());
			e.printStackTrace();
			System.exit(-2);
		} finally {
			log.info("Closing hibernate session");
			if(sessionFactory != null) sessionFactory.close();
		}
	}

	private static void performAnalyses(Collection<AnalysisMethod> methods, SessionFactory sessionFactory, Experiment exp, Collection<Pathway> pathways) throws AtlasException {
		for(Factor factor : exp.getFactors()) {
			for(AnalysisMethod m : methods) {
				Session session = sessionFactory.openSession();
				Transaction tx = session.beginTransaction();

				log.info("Performing " + m.getType() + " for " + factor.getName() + " / " + factor.getValue());
				//Get an existing analysis and update
				ExperimentAnalysis analysis = (ExperimentAnalysis)session.get(
						ExperimentAnalysis.class, 
						new ExperimentAnalysis.PrimaryKey(exp, factor, m.getType())
				);
				//Or create a new one
				if(analysis == null) analysis = new ExperimentAnalysis(exp, factor, m.getType());

				m.performAnalysis(analysis, pathways);
				
				session.merge(analysis);
				tx.commit();
			}
		}
	}
}
