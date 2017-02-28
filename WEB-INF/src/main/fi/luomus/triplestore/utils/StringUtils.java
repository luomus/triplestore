package fi.luomus.triplestore.utils;

import java.io.UnsupportedEncodingException;

public class StringUtils {

	public static String trimToByteLength(String s, int size) {
		if (s == null) return null;
		byte[] utf8 = toUTF8Bytes(s);
		while (utf8.length > size) {
			s = s.substring(0, s.length() - 10);
			utf8 = toUTF8Bytes(s);
		}
		return s;
	}

	private static byte[] toUTF8Bytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int countOfUTF8Bytes(String s) {
		return toUTF8Bytes(s).length;
	}
	
}
