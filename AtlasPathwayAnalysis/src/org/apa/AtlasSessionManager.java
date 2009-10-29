package org.apa;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apa.data.Experiment;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Factor;
import org.apa.data.FactorValue;
import org.apa.data.Pathway;
import org.apa.rest.AtlasExperimentData;
import org.apa.rest.AtlasRestCache;
import org.apa.rest.AtlasRestUtils;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.bio.Organism;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.pathvisio.wikipathways.WikiPathwaysCache;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;

public class AtlasSessionManager {
	Logger log = Logger.getLogger("org.apa");
	
	SessionFactory sessionFactory;

	public AtlasSessionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * Checks atlas for new experiments and updates them
	 * in the hibernate session.
	 * @throws AtlasException 
	 */
	public void updateAtlas(AtlasRestCache cache, Collection<String> ignoreExperiments) throws AtlasException {
		log.info("Updating Atlas data");
		try {
//			Set<String> atlasExperiments = AtlasRestUtils.listExperiments();
			//TESTING
			Set<String> atlasExperiments = new HashSet<String>();
			atlasExperiments.add("E-AFMX-1");
			atlasExperiments.add("E-AFMX-4");
			//atlasExperiments.add("E-AFMX-5");
			atlasExperiments.add("E-AFMX-6");
			atlasExperiments.add("E-AFMX-7");
			atlasExperiments.add("E-GEOD-571");
			atlasExperiments.add("E-GEOD-755");
			atlasExperiments.add("E-GEOD-758");
			atlasExperiments.add("E-GEOD-994");
			atlasExperiments.add("E-GEOD-995");
			atlasExperiments.add("E-GEOD-1036");
			atlasExperiments.add("E-GEOD-1045");
			atlasExperiments.add("E-GEOD-1432");
			atlasExperiments.add("E-GEOD-1437");
			atlasExperiments.add("E-GEOD-1462");
			atlasExperiments.add("E-GEOD-1463");
			atlasExperiments.add("E-GEOD-1472");
			atlasExperiments.add("E-GEOD-1474");
			atlasExperiments.add("E-GEOD-1479");
			atlasExperiments.add("E-GEOD-1482");
			atlasExperiments.add("E-GEOD-1493");
			atlasExperiments.add("E-GEOD-1639");
			atlasExperiments.add("E-GEOD-1648");
			atlasExperiments.add("E-GEOD-1832");
			atlasExperiments.add("E-GEOD-1837");
			atlasExperiments.add("E-GEOD-1839");
			atlasExperiments.add("E-GEOD-1843");
			atlasExperiments.add("E-GEOD-1849");
			atlasExperiments.add("E-GEOD-2210");
			atlasExperiments.add("E-GEOD-2405");
			atlasExperiments.add("E-GEOD-2407");
			atlasExperiments.add("E-GEOD-2411");
			atlasExperiments.add("E-GEOD-2413");
			atlasExperiments.add("E-GEOD-2431");
			atlasExperiments.add("E-GEOD-2433");
			atlasExperiments.add("E-GEOD-2437");
			atlasExperiments.add("E-GEOD-2487");
			atlasExperiments.add("E-GEOD-2489");
			atlasExperiments.add("E-GEOD-2499");
			atlasExperiments.add("E-GEOD-2602");
			atlasExperiments.add("E-GEOD-2683");
			atlasExperiments.add("E-GEOD-2685");
			atlasExperiments.add("E-GEOD-2882");
			atlasExperiments.add("E-GEOD-2883");
			atlasExperiments.add("E-GEOD-2884");
			atlasExperiments.add("E-GEOD-2889");
			
			Collection<String> sessionExperiments = AtlasSessionUtils.getExperiments(sessionFactory.getCurrentSession());
			
			//Add new experiments
			int i = 0;
			for(String id : atlasExperiments) {
				log.fine("Processing " + id);
				if(!sessionExperiments.contains(id) && !ignoreExperiments.contains(id)) {
					Session session = sessionFactory.openSession();
					Transaction tx = session.beginTransaction();

					log.fine("Adding experiment " + id);
					AtlasExperimentData restExp = cache.getExperimentData(id);
					if(cache.getOrganism(id) == null) {
						log.warning("Skipping experiment " + id + ", organism unkown");
						continue;
					}
					Experiment exp = AtlasRestUtils.asExperiment(
							restExp, 
							cache.getOrganism(id),
							cache.getName(id)
					);
					
					for(FactorValue fv : exp.getFactorValues()) {
						if(session.get(FactorValue.class, fv.getId()) == null) {
							if(session.get(Factor.class, fv.getFactor().getName()) == null) {
								session.persist(fv.getFactor());
							}
							session.persist(fv);
						}
					}
					session.persist(exp);
					
					log.info("Memory: " + Runtime.getRuntime().totalMemory() / 1E6);
					log.info(i++ + " committing transaction");
					log.info("Memory: " + Runtime.getRuntime().totalMemory() / 1E6);
					tx.commit();
					
				}
				if(i > 50) break; //testing
			}
			//Remove experiments that are not available anymore
			log.info("Removing unavailable experiments");
			for(String id : sessionExperiments) {
				log.fine("Processing " + id);
				if(!atlasExperiments.contains(id) && !ignoreExperiments.contains(id)) {
					log.fine("Removing experiment " + id);
					Session session = sessionFactory.openSession();
					Transaction tx = session.beginTransaction();
					
					//Remove analyses
					Collection<ExperimentAnalysis> analyses = AtlasSessionUtils.getAnalyses(sessionFactory.getCurrentSession(), id);
					for(ExperimentAnalysis a : analyses) session.delete(a);
					//Remove experiment
					Experiment exp = (Experiment)session.get(Experiment.class, id);
					session.delete(exp);
					
					tx.commit();
				}
			}
			
		} catch(Exception e) {
			if(e instanceof AtlasException) throw (AtlasException)e;
			else throw new AtlasException(e);
		}
	}
	
	/**
	 * Checks wikipathways for new pathways and updates them
	 * in the hibernate session.
	 * @throws AtlasException 
	 */
	public void updateWikiPathways(WikiPathwaysCache cache, IDMapper idMapper) throws AtlasException {
		log.info("Updating WikiPathways data");
		try {
			//cache.update();
			List<File> pathwayFiles = cache.getFiles();
			Collection<String> sessionPathways = AtlasSessionUtils.getPathways(sessionFactory.getCurrentSession());
			
			Set<String> pathwayIds = new HashSet<String>();

			//Check for pathways that are not in the session yet
			//int i = 0;
			for(File f : pathwayFiles) {
				WSPathwayInfo info = cache.getPathwayInfo(f);
				pathwayIds.add(info.getId());
				if(!sessionPathways.contains(info.getId())) {
					log.fine("Adding pathway " + info.getId());
					Session session = sessionFactory.openSession();
					Transaction tx = session.beginTransaction();
					
					Organism organism = Organism.fromLatinName(info.getSpecies());
					DataSource ds = AtlasRestUtils.getResultDatasource(organism);
					Pathway pathway = PathwayUtils.fromGpml(info, f, idMapper, ds);
					
					session.persist(pathway);
					tx.commit();
					
				}
				//if(i++ > 20) break; //Testing
			}
			
			//Check for removed pathways
			for(String id : sessionPathways) {
				if(!pathwayIds.contains(id)) {
					Session session = sessionFactory.openSession();
					Transaction tx = session.beginTransaction();
					
					log.fine("Removing pathway " + id);
					//Remove the pathway from the analyses
					//TODO: figure out if this is necessary
					//shouldn't be, because of cascade settings
					
					//Remove the pathway
					Pathway pathway = (Pathway)session.get(Pathway.class, id);
					session.delete(pathway);
					
					tx.commit();
				}
			}
		} catch(Exception e) {
			if(e instanceof AtlasException) throw (AtlasException)e;
			else throw new AtlasException(e);
		}
	}
}
