package org.apa.ae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.pathvisio.util.FileUtils;

public class ArrayDefinitionCache {
	private static final String URL_BASE = "http://www.ebi.ac.uk/microarray-as/ae/files/";

	File cacheDir;

	public ArrayDefinitionCache(File cacheDir) {
		this.cacheDir = cacheDir;
		cacheDir.mkdirs();
	}

	public ArrayDefinition get(String id) throws FileNotFoundException, IOException {
		File cacheFile = new File(cacheDir, id);
		if(!cacheFile.exists()) {
			//Save the cache
			saveUrlToFile(cacheFile, URL_BASE + id + "/" + id + ".adf.txt");
		}
		return new ArrayDefinition(id, new BufferedReader(new FileReader(cacheFile)));
	}

	private void saveUrlToFile(File saveFile,String location) throws MalformedURLException, IOException{
		FileUtils.downloadFile(new URL(location), saveFile);
	}
}
