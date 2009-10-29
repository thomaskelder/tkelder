package org.pathwaystats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Multimap;

public class EnrichmentUtils {
	public static <K, V> void permuteMap (Multimap<K, V> map) {
		List<Collection<V>> values = new ArrayList<Collection<V>>(map.asMap().values());
		HashSet<K> keys = new HashSet<K>(map.keySet());
		
		Collections.shuffle(values);
		
		int i = 0;
		for (K key : keys) {
			map.removeAll(key);
			map.putAll(key, values.get(i));
			i++;
		}
	}
}
