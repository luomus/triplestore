package fi.luomus.triplestore.taxonomy.runnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import fi.luomus.commons.utils.FileUtils;

public class FrequencyPointsFromMXCodeList {

	private static final Map<String, String> ADDITIONAL;
	static {
		ADDITIONAL = new HashMap<>();
		ADDITIONAL.put("tähti", "MX.typeOfOccurrenceOldRecords");
		ADDITIONAL.put("a", "MX.typeOfOccurrenceVagrant");
		ADDITIONAL.put("i", "MX.typeOfOccurrenceImport");
	}

	public static void main(String[] args) {
		try {
			generateStatements();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

	private static void generateStatements() throws Exception {
		for (String line : FileUtils.readLines(new File("C:/temp/frekvenssit/xxxx.txt"))) {
			String[] parts = line.split(Pattern.quote("\t"));
			generateStatements(parts);
		}
	}

	private static void generateStatements(String[] parts) {
		String taxonId = val(parts, 0);
		String points = val(parts, 1);
		String additional = val(parts, 2);
		generateStatements(taxonId, points, additional);
	}

	private static void generateStatements(String taxonId, String points, String additional) {
		if (points == null) return;
		Integer i = toInt(points);
		if (i == null) return;
		String enumValue = toEnumValue(i);
		if (enumValue == null) return;
		generateStatement(taxonId, i);
		generateStatement(taxonId, enumValue);
		String additionalEnumValue = ADDITIONAL.get(additional);
		if (additionalEnumValue != null) generateStatement(taxonId, additionalEnumValue);
	}

	private static void generateStatement(String taxonId, Integer frequencePoints) {
		System.out.println("exec addstatementL('"+taxonId+"', 'MX.frequencyScoringPoints', '"+frequencePoints+"');");
	}

	private static void generateStatement(String taxonId, String enumValue) {
		System.out.println("exec addstatement('"+taxonId+"', 'MX.typeOfOccurrenceInFinland', '"+enumValue+"');");
	}

	public static String toEnumValue(int i) {
		if (i < 1) return null;
		if (i < 30) return "MX.typeOfOccurrenceCommon";
		if (i < 60) return "MX.typeOfOccurrenceRare";
		return "MX.typeOfOccurrenceVeryRare";
	}

	private static Integer toInt(String points) {
		try {
			return Integer.valueOf(points);
		} catch (Exception e) {
			return null;
		}
	}

	private static String val(String[] parts, int i) {
		try {
			return parts[i];
		} catch (Exception e) {
			return null;
		}
	}

}
