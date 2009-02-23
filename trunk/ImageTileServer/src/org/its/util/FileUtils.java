package org.its.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
	public static byte[] readFile(File file) throws IOException {
		int len = (int)(file.length());
		FileInputStream fis = new FileInputStream(file);
		byte[] buf = new byte[len];
		fis.read(buf);
		fis.close();
		return buf;
	}
}
