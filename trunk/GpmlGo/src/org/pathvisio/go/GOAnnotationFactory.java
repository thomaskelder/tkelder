package org.pathvisio.go;

import java.util.Collection;

public interface GOAnnotationFactory<K extends GOAnnotation> {
	public Collection<K> createAnnotations(String id, String evidence);
}
