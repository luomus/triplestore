package fi.luomus.triplestore.taxonomy.iucn.runnable;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public class IUCN2010Sisaan {

	private static final String BLABLABLA = "blablabla blablabla blablabla blablabla blablabla";
	private static final String OCCURRENCES = "occurrences";
	private static final Map<String, Set<Qname>> FILE_TO_INFORMAL_GROUP;
	static {
		FILE_TO_INFORMAL_GROUP = new HashMap<String, Set<Qname>>();
		FILE_TO_INFORMAL_GROUP.put("Helttasienet_siirto.csv", Utils.set(new Qname("MVL.61")));
		FILE_TO_INFORMAL_GROUP.put("Hämähäkit_siirto.csv", Utils.set(new Qname("MVL.38")));
		FILE_TO_INFORMAL_GROUP.put("Jäkälät_siirto.csv", Utils.set(new Qname("MVL.25")));
		FILE_TO_INFORMAL_GROUP.put("Jäytiäiset_siirto.csv", Utils.set(new Qname("MVL.227")));
		FILE_TO_INFORMAL_GROUP.put("Kalat_siirto.csv", Utils.set(new Qname("MVL.27")));
		FILE_TO_INFORMAL_GROUP.put("Kierresiipiset_siirto.csv", Utils.set(new Qname("MVL.229")));
		FILE_TO_INFORMAL_GROUP.put("Kolmisukahäntäiset_siirto.csv", Utils.set(new Qname("MVL.301")));
		FILE_TO_INFORMAL_GROUP.put("Korennot_siirto.csv", Utils.set(new Qname("MVL.36")));
		FILE_TO_INFORMAL_GROUP.put("Kotelosienet_siirto.csv", Utils.set(new Qname("MVL.101")));
		FILE_TO_INFORMAL_GROUP.put("Kovakuoriaiset_siirto.csv", Utils.set(new Qname("MVL.33")));
		FILE_TO_INFORMAL_GROUP.put("Kupusienet_siirto.csv", Utils.set(new Qname("MVL.81")));
		FILE_TO_INFORMAL_GROUP.put("Kärpäset_siirto.csv", Utils.set(new Qname("MVL.224")));
		FILE_TO_INFORMAL_GROUP.put("Kääväkkäät_siirto.csv", Utils.set(new Qname("MVL.121")));
		FILE_TO_INFORMAL_GROUP.put("Levät_siirto.csv", Utils.set(new Qname("MVL.22")));
		FILE_TO_INFORMAL_GROUP.put("Limasienet_siirto.csv", null); // TODO
		FILE_TO_INFORMAL_GROUP.put("Linnut_siirto.csv", Utils.set(new Qname("MVL.1")));
		FILE_TO_INFORMAL_GROUP.put("Maasiirat_lukit_valeskorpionit_siirto.csv", Utils.set(new Qname("MVL.215"), new Qname("MVL.235"), new Qname("MVL.236")));
		FILE_TO_INFORMAL_GROUP.put("Matelijat_sammakot_siirto.csv", Utils.set(new Qname("MVL.26")));
		FILE_TO_INFORMAL_GROUP.put("Nilviäiset_siirto.csv", Utils.set(new Qname("MVL.239"), new Qname("MVL.240")));
		FILE_TO_INFORMAL_GROUP.put("Nisäkkäät_siirto.csv", Utils.set(new Qname("MVL.2")));
		FILE_TO_INFORMAL_GROUP.put("Nivelkärsäiset_siirto.csv", Utils.set(new Qname("MVL.34")));
		FILE_TO_INFORMAL_GROUP.put("Nivelmadot_siirto.csv", Utils.set(new Qname("MVL.241")));
		FILE_TO_INFORMAL_GROUP.put("Perhoset_siirto.csv", Utils.set(new Qname("MVL.31")));
		FILE_TO_INFORMAL_GROUP.put("Piensienet_siirto.csv", Utils.set(new Qname("MVL.233"))); // TODO tällaista ryhmää ei ole, liitetty sienien yläryhmään
		FILE_TO_INFORMAL_GROUP.put("Pistiäiset_siirto.csv", Utils.set(new Qname("MVL.")));
		FILE_TO_INFORMAL_GROUP.put("Punkit_siirto.csv", Utils.set(new Qname("MVL.")));
		FILE_TO_INFORMAL_GROUP.put("Putkilokasvit_siirto.csv", Utils.set(new Qname("MVL.281"), new Qname("MVL.282")));
		FILE_TO_INFORMAL_GROUP.put("Ripsiäiset_siirto.csv", Utils.set(new Qname("MVL.228")));
		FILE_TO_INFORMAL_GROUP.put("Sammalet_siirto.csv", Utils.set(new Qname("MVL.23")));
		FILE_TO_INFORMAL_GROUP.put("Suorasiipiset_siirto.csv", Utils.set(new Qname("MVL.223")));
		FILE_TO_INFORMAL_GROUP.put("Sääsket_siirto.csv", Utils.set(new Qname("MVL.224")));
		FILE_TO_INFORMAL_GROUP.put("Tuhatjalkaiset_siirto.csv", Utils.set(new Qname("MVL.37")));
		FILE_TO_INFORMAL_GROUP.put("Verkkosiipiset_siirto.csv", Utils.set(new Qname("MVL.226")));
		FILE_TO_INFORMAL_GROUP.put("Vesiperhoset_siirto.csv", Utils.set(new Qname("MVL.222")));
	}
	public static void main(String[] args) {
		try {
			process();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

	private static void process() throws Exception {
		File folder = new File("C:/Users/Zz/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/2010");
		for (File f : folder.listFiles()) {
			if (!f.isFile()) continue;
			if (!f.getName().endsWith(".csv")) continue;
			System.out.println(f.getName());
			if (!"Hämähäkit_siirto.csv".equals(f.getName())) continue; // XXX
			process(f);
		}
		//writeDumps();
	}

	private static void process(File f) throws Exception {
		for (String line : FileUtils.readLines(f)) {
			line = line.trim();
			if (line.isEmpty()) continue;
			process(line);
		}
	}

	private static void process(String line) throws Exception {
		String[] parts = line.split(Pattern.quote("|"));
		IUCNLineData data = new IUCNLineData(parts);
		dump(data);		
	}

	private static final Map<String, Map<String, Integer>> dumps = new HashMap<>();
	private static final Set<String>IGNORE = Utils.set("taxonomicNotes", "scientificName", "lastSightingNotes", "finnishName", "legacyPublications", "occurrenceNotes", "habitatNotes", "groundsForEvaluationNotes");
	
	private static void dump(IUCNLineData data) throws Exception {
		for (Field field : data.getClass().getFields()) {
			String fieldname = field.getName();
			if (IGNORE.contains(fieldname)) continue;
			if (!dumps.containsKey(fieldname)) dumps.put(fieldname, new HashMap<String, Integer>());
			Object o = field.get(data);
			if (o == null || o instanceof String) {
				String value = o == null ? "" : (String) o;
				if (value.length() > 30) value = BLABLABLA;
				if (!dumps.get(fieldname).containsKey(value)) {
					dumps.get(fieldname).put(value, 1);
				} else {
					dumps.get(fieldname).put(value, dumps.get(fieldname).get(value) + 1);
				}
			}
		}
		if (!dumps.containsKey(OCCURRENCES)) dumps.put(OCCURRENCES, new HashMap<String, Integer>());
		Map<String, Integer> occurrenceDump = dumps.get(OCCURRENCES);
		for (String occurrence : data.occurences.values()) {
			if (!occurrenceDump.containsKey(occurrence)) {
				occurrenceDump.put(occurrence, 1);
			} else {
				occurrenceDump.put(occurrence, occurrenceDump.get(occurrence) + 1);
			}
		}
	}

	@SuppressWarnings("unused")
	private static void writeDumps() throws IOException {
		for (String field : dumps.keySet()) {
			writeDump(field, dumps.get(field));
		}
	}

	private static void writeDump(String field, Map<String, Integer> map) throws IOException {
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>();
		sorted.addAll(map.entrySet());
		Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		File file = new File("C:/temp/iucn/"+field+".txt");
		for (Map.Entry<String, Integer> e : sorted) {
			FileUtils.writeToFile(file, Utils.debugS(e.getKey(), e.getValue())+"\n", true);
		}
	}


}
