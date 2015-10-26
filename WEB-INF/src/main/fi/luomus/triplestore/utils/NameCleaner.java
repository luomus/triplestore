package fi.luomus.triplestore.utils;

import java.util.regex.Pattern;

/**
 * Used in templates to clean for example "MX.scientificName" -> "scientificName"
 *
 */
public class NameCleaner {

	public static String clean(String name) {
		if (name.contains(":")) {
			return name.split(Pattern.quote(":"))[1];
		}
		if (name.contains(".")) {
			return name.split(Pattern.quote("."))[1];
		}
		throw new IllegalArgumentException("Unable to clean name " + name);
	}

}
