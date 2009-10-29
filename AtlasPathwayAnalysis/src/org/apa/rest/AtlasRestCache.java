package org.apa.rest;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import org.apa.AtlasException;
import org.apa.ae.ArrayDefinitionCache;
import org.apa.ae.ExperimentInfo;
import org.apa.ae.ExperimentInfoCache;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.Xref;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.DataDerby;
import org.codehaus.jackson.map.ObjectMapper;
import org.pathvisio.gex.SimpleGex;

public class AtlasRestCache {
	Logger log = Logger.getLogger("org.apa.rest");
	private static ObjectMapper jsonMapper = new ObjectMapper();

	//ArrayExpress specific caches
	ArrayDefinitionCache adCache;
	ExperimentInfoCache expCache;

	File cacheDir;
	IDMapper idMapper;
	
	public AtlasRestCache(File cacheDir, IDMapper idMapper) {
		this.idMapper = idMapper;
		this.cacheDir = new File(cacheDir, "atlas");
		this.cacheDir.mkdirs();
		adCache = new ArrayDefinitionCache(new File(cacheDir, "arraydef"));
		expCache = new ExperimentInfoCache(new File(cacheDir, "expinfo"));
	}

	public SimpleGex getGex(String accession) throws AtlasException {
		try {
			File cacheFile = new File(cacheDir, accession + ".pgex");
			SimpleGex gex = null;
			if(cacheFile.exists()) {
				gex = new SimpleGex(cacheFile.getAbsolutePath(), false, new DataDerby());
			} else {
				Organism org = Organism.fromLatinName(expCache.get(accession).getOrganism());
				gex = AtlasRestUtils.asGex(cacheFile.getParentFile(), getExperimentData(accession), org);
			}
			return gex;
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}

	public Organism getOrganism(String accession) throws AtlasException {
		try {
			ExperimentInfo info = expCache.get(accession);
			String orgName = info.getOrganism();
			Organism org = Organism.fromLatinName(orgName);
			if(org == null) {
				log.warning("Unknown organism: " + orgName);
			}
			return org;
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}

	public AtlasExperiment getExperiment(String accession) throws AtlasException {
		try {
			File cacheFile = new File(cacheDir, accession + ".exp");
			AtlasExperiment exp = null;
			if(cacheFile.exists()) {
				exp = jsonMapper.readValue(cacheFile, AtlasExperiment.class);
			} else {
				exp = AtlasRestUtils.loadExperiment(accession);
				jsonMapper.writeValue(cacheFile, exp);
			}
			return exp;
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}

	public AtlasExperimentData getExperimentData(String accession) throws AtlasException {
		try {
			File cacheFile = new File(cacheDir, accession + ".data");
			AtlasExperimentData data = null;
			if(cacheFile.exists()) {
				data = jsonMapper.readValue(cacheFile, AtlasExperimentData.class);
			} else {
				Organism org = getOrganism(accession);
				DataSource targetDs = AtlasRestUtils.getQueryDatasource(org);
				Set<Xref> genes = AtlasRestUtils.getAllExperimentGenes(
						getExperiment(accession), adCache, targetDs, idMapper);
				data = AtlasRestUtils.loadExperimentData(accession, genes);
				jsonMapper.writeValue(cacheFile, data);
			}
			return data;
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}
}
