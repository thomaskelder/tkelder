package org.apa.ae;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ArrayDefinition {
	//TODO: convert to regex
	private static final HashMap<String, DataSource> colContains = new HashMap<String, DataSource>();
	static {
		colContains.put("ensembl", BioDataSource.ENSEMBL);
		colContains.put("refseq", BioDataSource.REFSEQ);
		colContains.put("tair", BioDataSource.TAIR);
		colContains.put("flybase", BioDataSource.FLYBASE);
		colContains.put("CompositeSequence Name", BioDataSource.AFFY);
	}

	String id;
	Multimap<DataSource, Xref> xrefs = new HashMultimap<DataSource, Xref>();
	
	public ArrayDefinition(String id, BufferedReader in) throws IOException {
		this.id = id;
		
		String line = in.readLine();
		String[] headers = line.split("\t");
		Map<Integer, DataSource> colNr2ds = new HashMap<Integer, DataSource>();
		
		for(int i = 0; i < headers.length; i++) {
			for(String contains : colContains.keySet()) {
				if(headers[i].contains(contains)) {
					colNr2ds.put(i, colContains.get(contains));
					break;
				}
			}
		}
		
		if(colNr2ds.size() == 0) {
			throw new IllegalArgumentException("No datasource recognized in: " + line);
		}
		
		while((line = in.readLine()) != null) {
			String[] ids = line.split("\t", -1);
			for(int i = 0; i < ids.length; i++) {
				DataSource ds = colNr2ds.get(i);
				if(ids[i] != null && !"".equals(ids[i]) && ds != null) {
					xrefs.put(ds, new Xref(ids[i], ds));
				}
			}
		}
		
		in.close();
	}
	
	public Set<Xref> getXrefs(DataSource dataSource) {
		Set<Xref> set = new HashSet<Xref>();
		if(dataSource.getFullName().startsWith("Ensembl")) {
			//Hack to make species specific ensembl work
			Collection<Xref> ensRefs = xrefs.get(BioDataSource.ENSEMBL);
			for(Xref x : ensRefs) {
				set.add(new Xref(x.getId(), dataSource));
			}
		} else {
			if(xrefs.containsKey(dataSource)) {
				set.addAll(xrefs.get(dataSource));
			}
		}
		return set;
	}
}
