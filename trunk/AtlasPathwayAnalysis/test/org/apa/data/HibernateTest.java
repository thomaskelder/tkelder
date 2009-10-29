package org.apa.data;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apa.AtlasSessionUtils;
import org.apa.rest.AtlasExperimentData;
import org.apa.rest.AtlasRestCache;
import org.apa.rest.AtlasRestUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HibernateTest extends TestCase {
	private static final SessionFactory sessionFactory = buildSessionFactory();

	private static SessionFactory buildSessionFactory() {
		try {
			return AtlasSessionUtils.createSessionFactory(new File("test/org/apa/data/hibernate.cfg.xml"));
		}
		catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	static final String EXPERIMENT_ID = "testExperiment";
	static final String EXPERIMENT_DESCR = "Test experiment...";
	static final String[] factorNames = new String[] { "testFactor1", "testFactor2" };
	static final String[][] factorValues = new String[][] {
		{ "testValue1", "testValue2" },
		{ "testValue3", "testValue4" }
	};

	public void testAddExperiment() {
		try {
			Session session = getSessionFactory().getCurrentSession();
			session.beginTransaction();

			Experiment exp = new Experiment(EXPERIMENT_ID);
			exp.setDesciption(EXPERIMENT_DESCR);

			Set<Factor> factors = new HashSet<Factor>();

			for(int i = 0; i < factorNames.length; i++) {
				for(int j = 0; j < factorValues[i].length; j++) {
					Factor f = new Factor(factorNames[i], factorValues[i][j]);
					factors.add(f);
					exp.addFactor(f);
					session.persist(f);
				}
			}
			
			Set<Pathway> pathways = new HashSet<Pathway>();
			for(int i = 0; i < 3; i++) {
				Pathway p = new Pathway("pathway" + i);
				p.setOrganism("testOrganism");
				Set<String> genes = new HashSet<String>();
				genes.add("testGene1");
				genes.add("testGene2");
				p.setGenes(genes);
				p.setUrl("http://test.com/pathway" + i);
				p.setLocalFile(new File("/tmp/test.gpml").getAbsolutePath());
				pathways.add(p);
				session.persist(p);
			}
			
			for(int i = 0; i < factorNames.length; i++) {
				for(int j = 0; j < factorValues[i].length; j++) {
					Factor f = new Factor(factorNames[i], factorValues[i][j]);
					ExperimentData d = new ExperimentData(exp, f);
					d.addEntry("gene1", 0.1, 1, 10);
					d.addEntry("gene2", 0.1, 1, 10);
					exp.setData(f, d);
				}
			}
			session.persist(exp);

			for(Factor f : factors) {
				ExperimentAnalysis analysis = new ExperimentAnalysis(exp, f, "test");
				for(Pathway p : pathways) {
					Statistic stat = new Statistic(p, analysis);
					analysis.setStatistic(stat);
				}
				session.persist(analysis);
			}
			
			session.getTransaction().commit();
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetExperiment() {
		try {
			Session session = getSessionFactory().getCurrentSession();
			session.beginTransaction();

			Experiment exp = (Experiment)session.load(Experiment.class, EXPERIMENT_ID);
			assertEquals(exp.getAccession(), EXPERIMENT_ID);
			assertEquals(exp.getDesciption(), EXPERIMENT_DESCR);
			for(int i = 0; i < factorNames.length; i++) {
				for(int j = 0; j < factorValues[i].length; j++) {
					Factor f = new Factor(factorNames[i], factorValues[i][j]);
					assertTrue("Factor " + f + " missing from " + exp.getFactors(), exp.getFactors().contains(f));
					assertNotNull(exp.getData(f));
				}
			}
			
			Pathway p = (Pathway)session.load(Pathway.class, "pathway0");
			assertNotNull(p);
			assertEquals(4, p.getStatistics().size());
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testListExperiments() {
		try {
			Session session = getSessionFactory().getCurrentSession();
			session.beginTransaction();

			Collection<String> experiments = AtlasSessionUtils.getExperiments(session);
			assertEquals(1, experiments.size());
			
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testListAnalyses() {
		try {
			Session session = getSessionFactory().getCurrentSession();
			session.beginTransaction();

			Collection<ExperimentAnalysis> analyses = AtlasSessionUtils.getAnalyses(session, EXPERIMENT_ID);
			assertEquals(4, analyses.size());
			
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	AtlasRestCache cache = new AtlasRestCache(new File("/home/thomas/data/atlas/"), null);
	static final String REST_EXPERIMENT_ID = "E-TABM-34";

	public void testFromRest() {
		try {
			AtlasExperimentData restExp = cache.getExperimentData(REST_EXPERIMENT_ID);
			Experiment exp = AtlasRestUtils.asExperiment(restExp, cache.getOrganism(REST_EXPERIMENT_ID));
			
			Session session = getSessionFactory().getCurrentSession();
			session.beginTransaction();
			for(Factor f : exp.getFactors()) {
				session.persist(f);
			}
			session.persist(exp);
			session.getTransaction().commit();
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
