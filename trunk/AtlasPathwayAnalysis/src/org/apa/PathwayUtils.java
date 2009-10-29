package org.apa;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apa.data.Pathway;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.model.ConverterException;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;

public class PathwayUtils {
	public static Pathway fromGpml(WSPathwayInfo info, File gpmlFile, IDMapper idMapper, DataSource targetDs) throws ConverterException, IDMapperException {
		org.pathvisio.model.Pathway gpmlPathway = new org.pathvisio.model.Pathway();
		gpmlPathway.readFromXml(gpmlFile, true);
		
		Pathway pathway = new Pathway(info.getId());
		pathway.setLocalFile(gpmlFile.getAbsolutePath());
		pathway.setUrl(info.getUrl());
		pathway.setName(info.getName());
		pathway.setOrganism(info.getSpecies());
		
		Set<String> genes = new HashSet<String>();
		for(Xref x : gpmlPathway.getDataNodeXrefs()) {
			if(x.getId() == null || x.getDataSource() == null) continue;
			for(Xref xx : idMapper.mapID(x, targetDs)) {
				genes.add(xx.getId());
			}
		}
		
		pathway.setGenes(genes);
		
		return pathway;
	}
}
