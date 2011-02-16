package org.pct.util;

import java.util.Set;

import org.bridgedb.AttributeMapper;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;

public class BridgeDbUtils {
	public static String getSymbol(Xref x, IDMapper idm) throws IDMapperException {
		String symbol = null;
		
		if(idm instanceof AttributeMapper) {
			AttributeMapper am = (AttributeMapper)idm;
			
			symbol = findSymbol(x, am);
			if(symbol == null) {
				//Map to a datasource for which a symbol is usually present
				for(Xref xx : idm.mapID(x, BioDataSource.ENTREZ_GENE)) {
					symbol = findSymbol(xx, am);
					if(symbol != null) break;
				}
			}
		}
		
		return symbol;
	}
	
	private static String findSymbol(Xref x, AttributeMapper am) throws IDMapperException {
		String s = null;
		Set<String> options = am.getAttributes(x, "Symbol");
		if(options == null) {
			for(String o : options) {
				s = o;
				if(o.matches("^[A-Za-z]{1}")) break;
			}
		}
		return s;
	}
}
