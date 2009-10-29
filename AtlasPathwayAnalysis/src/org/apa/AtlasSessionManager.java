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
import org.apa.data.Pathway;
import org.apa.rest.AtlasExperimentData;
import org.apa.rest.AtlasRestCache;
import org.apa.rest.AtlasRestUtils;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.bio.Organism;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
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
			Set<String> atlasExperiments = AtlasRestUtils.listExperiments();
			Collection<String> sessionExperiments = AtlasSessionUtils.getExperiments(sessionFactory.getCurrentSession());
			
			//Add new experiments
			int i = 0; //testing
			for(String id : atlasExperiments) {
				if(!sessionExperiments.contains(id) && !ignoreExperiments.contains(id)) {
					Session session = sessionFactory.openSession();
					Transaction tx = session.beginTransaction();

					log.fine("Adding experiment " + id);
					AtlasExperimentData restExp = cache.getExperimentData(id);
					if(cache.getOrganism(id) == null) {
						log.warning("Skipping experiment " + id + ", organism unkown");
						continue;
					}
					Experiment exp = AtlasRestUtils.asExperiment(restExp, cache.getOrganism(id));
					
					for(Factor f : exp.getFactors()) {
						if(session.get(Factor.class, f.getId()) == null) session.persist(f);
					}
					session.persist(exp);
					
					log.info(i + " committing transaction");
					tx.commit();
					
				}
				if(i++ > 50) break; //testing
			}
			//Remove experiments that are not available anymore
			for(String id : sessionExperiments) {
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
