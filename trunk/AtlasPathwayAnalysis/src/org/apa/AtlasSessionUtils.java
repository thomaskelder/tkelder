package org.apa;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apa.data.Experiment;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.ExperimentData;
import org.apa.data.Factor;
import org.apa.data.Pathway;
import org.apa.data.Statistic;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

public class AtlasSessionUtils {
	@SuppressWarnings("unchecked")
	public static Collection<String> getExperiments(Session session) {
		List<String> list = (List<String>)session.createQuery(
				"select accession from Experiment").list();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<ExperimentAnalysis> getAnalysesByType(Session session, String type) {
		List<ExperimentAnalysis> list = (List<ExperimentAnalysis>)session.createQuery(
				"from ExperimentAnalysis ea where ea.id.type = ?")
			.setString(0, type)
			.list();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<ExperimentAnalysis> getAnalyses(Session session, String experimentAccession) {
		List<ExperimentAnalysis> list = (List<ExperimentAnalysis>)session.createQuery(
				"from ExperimentAnalysis where experiment_accession = ?")
			.setString(0, experimentAccession)
			.list();
		return list;
	}

	@SuppressWarnings("unchecked")
	public static Collection<String> getPathways(Session session) {
		List<String> list = (List<String>)session.createQuery("select id from Pathway").list();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<ExperimentAnalysis> getAnalyses(Session session) {
		List<ExperimentAnalysis> list = (List<ExperimentAnalysis>)session.createQuery("from ExperimentAnalysis").list();
		return list;
	}
	
	public static SessionFactory createSessionFactory(File configFile) {
		AnnotationConfiguration conf = new AnnotationConfiguration();
		conf.configure(configFile);
		conf.addAnnotatedClass(Experiment.class);
		conf.addAnnotatedClass(Factor.class);
		conf.addAnnotatedClass(ExperimentData.ExperimentDataEntry.class);
		conf.addAnnotatedClass(ExperimentData.class);
		conf.addAnnotatedClass(Pathway.class);
		conf.addAnnotatedClass(ExperimentAnalysis.class);
		conf.addAnnotatedClass(Statistic.class);
		
		return conf.buildSessionFactory();
	}
}
