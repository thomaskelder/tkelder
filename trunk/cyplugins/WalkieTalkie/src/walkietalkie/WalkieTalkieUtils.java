package walkietalkie;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.PathwayParser;
import org.pathvisio.util.PathwayParser.ParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import walkietalkie.WalkieTalkie.PathwayInfo;

/**
 * Task centered utility methods.
 * @author thomas
 *
 */
public class WalkieTalkieUtils {
	private WalkieTalkieUtils() {}

	public static Set<PathwayInfo> pathwaysByZScore(File dir, StatisticsResult stats, double minz) throws IDMapperException, SAXException, ParseException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		Set<PathwayInfo> pathways = new HashSet<PathwayInfo>();

		for(StatisticsPathwayResult pwr : stats.getPathwayResults()) {
			if(pwr.getZScore() >= minz) {
				PathwayParser pp = new PathwayParser(pwr.getFile(), xmlReader);
				pathways.add(new PathwayInfo(pwr.getFile(), pp.getName(), pp.getGenes()));
			}
		}
		return pathways;
	}

	public static Set<PathwayInfo> readPathways(File dir) throws SAXException, ParseException {
		//Read the pathway information
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		Set<PathwayInfo> pathways = new HashSet<PathwayInfo>();

		for(File f : FileUtils.getFiles(dir, "gpml", true)) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			pathways.add(new PathwayInfo(f, pp.getName(), pp.getGenes()));
		}
		return pathways;
	}
}
