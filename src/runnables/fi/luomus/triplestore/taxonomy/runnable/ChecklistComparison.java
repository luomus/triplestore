package fi.luomus.triplestore.taxonomy.runnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.Utils;

public class ChecklistComparison {

	private static final String PREV_YEAR = "2018";
	private static final String LATEST_YEAR = "2019";

	private static final String TAB = "\t";
	private static final String NEWLINE = "\n";

	public static void main(String[] args) {
		try {
			compareChecklists();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

	private static void compareChecklists() throws Exception {
		File latest = new File("C:/temp/Lajiluettelo2018/2019-11-21-taxon-export.tsv");
		File previous = new File("C:/temp/Lajiluettelo2018/Lajiluettelo2018.txt");
		String s = new ChecklistComparator().compare(
				new ChecklistReader().read(latest),
				new ChecklistReader().read(previous));
		FileUtils.writeToFile(new File("c:/temp/Lajiluettelo2018/checklist_comparison_+"+DateUtils.getFilenameDatetime()+".tsv"), s);
	}

	private static class ChecklistComparator {


		public String compare(Checklist latest, Checklist previous) {
			StringBuilder b = new StringBuilder();
			colHeaders(b);
			b.append(NEWLINE);
			latest.rows.values().forEach(row -> {
				ChecklistRow prevRow = previous.getRow(row.taxonId);
				if (!row.equals(prevRow)) {
					b.append(LATEST_YEAR).append(TAB);
					b.append(row.toString());
					b.append(NEWLINE);
					b.append(PREV_YEAR).append(TAB);
					if (prevRow != null) {
						b.append(prevRow.toString());
					}
					b.append(NEWLINE).append(NEWLINE);
				}
			});

			b.append(NEWLINE).append("Removed taxa").append(NEWLINE);
			colHeaders(b);
			b.append(NEWLINE);
			previous.rows.values().forEach(prevRow -> {
				if (!latest.contains(prevRow.taxonId)) {
					b.append(PREV_YEAR).append(TAB);
					b.append(prevRow.toString()).append(NEWLINE);
				}
			});

			return b.toString();
		}

		private void colHeaders(StringBuilder b) {
			b.append("Year").append(TAB);
			for (Field f : FIELDS.keySet()) {
				b.append(colName(f.getName())).append(TAB);
			}
		}

		private Object colName(String name) {
			return Utils.upperCaseFirst(name.replaceAll(
					String.format("%s|%s|%s",
							"(?<=[A-Z])(?=[A-Z][a-z])",
							"(?<=[^A-Z])(?=[A-Z])",
							"(?<=[A-Za-z])(?=[^A-Za-z])"
							),
					" "
					));
		}
	}

	private static class ChecklistReader {

		public Checklist read(File file) throws Exception {
			List<ChecklistRow> rows = readRows(file);
			return new Checklist(rows);
		}

		private List<ChecklistRow> readRows(File file) throws Exception, FileNotFoundException, IOException {
			System.out.println("Reading " + file.getName());
			List<ChecklistRow> rows = new ArrayList<>();
			CSVParser parser = readCSV(FileUtils.readContents(file));
			int i = 0;
			List<CSVRecord> records = parser.getRecords(); 
			for (CSVRecord record : records) {
				rows.add(parse(record));
				if (++i % 1000 == 0) System.out.println((i) + " / " + records.size());

			}
			System.out.println("Reading " + file.getName() +" done!");
			return rows;
		}

		private ChecklistRow parse(CSVRecord record) {
			ChecklistRow row = new ChecklistRow();
			for (Map.Entry<Field, String[]> e : FIELDS.entrySet())  {
				for (String colName : e.getValue()) {
					Field f = e.getKey();
					setValue(record, row, colName, f);
				}
			}
			return row;
		}

		private void setValue(CSVRecord record, ChecklistRow row, String colName, Field f) {
			String value = get(record, colName);
			if (value != null) try {
				f.set(row, value);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		private String get(CSVRecord record, String colName) {
			try {
				String s = record.get(colName);
				if (given(s)) return s;
			} catch (Exception e) {}
			return null;
		}

		private CSVParser readCSV(String data) throws Exception {
			CSVFormat csvFormat = CSVFormat.TDF.withFirstRecordAsHeader()
					.withIgnoreEmptyLines(true)
					.withTrim();
			return csvFormat.parse(new StringReader(data));
		}

	}

	private static class Checklist {
		private final Map<String, ChecklistRow> rows = new LinkedHashMap<>();
		public Checklist(List<ChecklistRow> rows) {
			rows.forEach(r->this.rows.put(r.taxonId, r));
		}
		public boolean contains(String taxonId) {
			return rows.containsKey(taxonId);
		}
		ChecklistRow getRow(String taxonId) {
			return rows.get(taxonId);
		}
	}

	private static final Map<Field, String[]> FIELDS;
	static {
		ArrayList<Field> ordered = new ArrayList<>();
		ordered.addAll(Arrays.asList(ChecklistRow.class.getFields()));
		Collections.sort(ordered, new Comparator<Field>() {
			@Override
			public int compare(Field o1, Field o2) {
				return Double.compare(o1.getAnnotation(FieldInfo.class).order(), o2.getAnnotation(FieldInfo.class).order());
			}
		});
		FIELDS = new LinkedHashMap<>();
		for (Field f : ordered) {
			FIELDS.put(f, f.getAnnotation(FieldInfo.class).cols());
		}
	}

	private static class ChecklistRow {
		@FieldInfo(order=0, cols={"TAXON_QNAME", "ID"}) public String taxonId;
		@FieldInfo(order=3.01, cols={"DOMAINNAME", "MX.domain, MX.scientificName"}) public String domainName;
		@FieldInfo(order=3.02, cols={"KINGDOMNAME", "MX.kingdom, MX.scientificName"}) public String kingdomName;
		@FieldInfo(order=3.03, cols={"PHYLUMNAME", "MX.phylum, MX.scientificName"}) public String phylumName;
		@FieldInfo(order=3.04, cols={"", "MX.division, MX.scientificName"}) public String divisionName;
		@FieldInfo(order=3.05, cols={"CLASSNAME", "MX.class, MX.scientificName"}) public String className;
		@FieldInfo(order=3.06, cols={"SUBCLASSNAME", "MX.subclass, MX.scientificName"}) public String subclassName;
		@FieldInfo(order=3.07, cols={"ORDERNAME", "MX.order, MX.scientificName"}) public String orderName;
		@FieldInfo(order=3.08, cols={"SUBORDERNAME", "MX.suborder, MX.scientificName"}) public String suborderName;
		@FieldInfo(order=3.09, cols={"SUPERFAMILYNAME", "MX.superfamily, MX.scientificName"}) public String superfamilyName;
		@FieldInfo(order=3.10, cols={"FAMILYNAME", "MX.family, MX.scientificName"}, useInCompare=true) public String familyName;
		@FieldInfo(order=3.11, cols={"SUBFAMILYNAME", "MX.subfamily, MX.scientificName"}) public String subfamilyName;
		@FieldInfo(order=3.12, cols={"TRIBENAME", "MX.tribe, MX.scientificName"}) public String tribeName;
		@FieldInfo(order=3.13, cols={"SUBTRIBENAME", "MX.subtribe, MX.scientificName"}) public String subtribeName;
		@FieldInfo(order=3.14, cols={"GENUSNAME", "MX.genus, MX.scientificName"}) public String genusName;
		@FieldInfo(order=3.15, cols={"SUBGENUSNAME", "MX.subgenus, MX.scientificName"}) public String subgenusName;
		@FieldInfo(order=1.1, cols={"SCIENTIFICNAME", "Scientific name"}, useInCompare=true) public String scientificName;
		@FieldInfo(order=1.2, cols={"AUTHORS", "Author"}, useInCompare=true) public String author;
		@FieldInfo(order=2.1, cols={"FINNISHNAME", "Finnish name"}, useInCompare=true) public String finnishName;
		@FieldInfo(order=2.2, cols={"SWEDISHNAME", "Swedish name"}, useInCompare=true) public String swedishName;

		private final Collection<Field> COMPARISON_FIELDS = FIELDS.keySet().stream().filter(f->f.getAnnotation(FieldInfo.class).useInCompare()).collect(Collectors.toList());

		public boolean equals(ChecklistRow other) {
			if (other == null) return false;
			return this.comparisonString().equals(other.comparisonString());
		}
		private String comparisonString = null;
		public String comparisonString() {
			if (comparisonString == null) comparisonString = toString(COMPARISON_FIELDS); 
			return comparisonString;
		}
		public String toString(Collection<Field> fields) {
			try {
				StringBuilder b = new StringBuilder();
				for (Field f : FIELDS.keySet()) {
					Object v = f.get(this);
					String s = v == null ? "" : v.toString(); 
					b.append(s).append(TAB);
				}
				return b.toString();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		private String toString = null;
		@Override
		public String toString() {
			if (toString == null) toString = toString(FIELDS.keySet()); 
			return toString;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldInfo {
		public double order();
		public String[] cols();
		public boolean useInCompare() default false;
	}

	public static boolean given(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
