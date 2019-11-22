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
		FileUtils.writeToFile(new File("c:/temp/Lajiluettelo2018/checklist_comparison_"+DateUtils.getFilenameDatetime()+".tsv"), s);
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
						b.append(prevRow.toString()).append(NEWLINE);
						b.append("Differences:").append(TAB).append(differences(row, prevRow));
					} else {
						b.append("new taxon");
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

		private String differences(ChecklistRow latest, ChecklistRow prevRow) {
			List<String> differences = new ArrayList<>();
			for (Field f : FIELDS.keySet()) {
				try {
					String valueOfLatest = (String) f.get(latest);
					String valueOfPrev = (String) f.get(prevRow);
					if (valueOfLatest == null) valueOfLatest = "";
					if (valueOfPrev == null) valueOfPrev = "";
					if (!valueOfLatest.equals(valueOfPrev)) {
						String colname = colName(f);
						if (!f.getAnnotation(FieldInfo.class).useInCompare()) colname = "(" + colname + ")";
						differences.add(colname);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return differences.stream().collect(Collectors.joining(", "));
		}
		private void colHeaders(StringBuilder b) {
			b.append("Year").append(TAB);
			for (Field f : FIELDS.keySet()) {
				b.append(colName(f)).append(TAB);
			}
		}

		private String colName(Field f) {
			return f.getAnnotation(FieldInfo.class).name();
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
		@FieldInfo(name="Id", order=0, cols={"TAXON_QNAME", "ID"}) public String taxonId;
		@FieldInfo(name="Scientific name", order=1.1, cols={"SCIENTIFICNAME", "Scientific name"}, useInCompare=true) public String scientificName;
		@FieldInfo(name="Author", order=1.2, cols={"AUTHORS", "Author"}, useInCompare=true) public String author;
		@FieldInfo(name="Finnish name", order=2.1, cols={"FINNISHNAME", "Finnish name"}, useInCompare=true) public String finnishName;
		@FieldInfo(name="Swedish name", order=2.2, cols={"SWEDISHNAME", "Swedish name"}, useInCompare=true) public String swedishName;
		@FieldInfo(name="Domain", order=3.01, cols={"DOMAINNAME", "MX.domain, MX.scientificName"}) public String domainName;
		@FieldInfo(name="Kingdom", order=3.02, cols={"KINGDOMNAME", "MX.kingdom, MX.scientificName"}) public String kingdomName;
		@FieldInfo(name="Phylum", order=3.031, cols={"PHYLUMNAME", "MX.phylum, MX.scientificName"}) public String phylumName;
		@FieldInfo(name="Subphylum", order=3.032, cols={"MX.subphylum, MX.scientificName"}) public String subphylumName;
		@FieldInfo(name="Division", order=3.04, cols={"", "MX.division, MX.scientificName"}) public String divisionName;
		@FieldInfo(name="Class", order=3.05, cols={"CLASSNAME", "MX.class, MX.scientificName"}) public String className;
		@FieldInfo(name="Subclass", order=3.06, cols={"SUBCLASSNAME", "MX.subclass, MX.scientificName"}) public String subclassName;
		@FieldInfo(name="Order", order=3.07, cols={"ORDERNAME", "MX.order, MX.scientificName"}) public String orderName;
		@FieldInfo(name="Suborder", order=3.08, cols={"SUBORDERNAME", "MX.suborder, MX.scientificName"}) public String suborderName;
		@FieldInfo(name="Syperfamily", order=3.09, cols={"SUPERFAMILYNAME", "MX.superfamily, MX.scientificName"}) public String superfamilyName;
		@FieldInfo(name="Family", order=3.10, cols={"FAMILYNAME", "MX.family, MX.scientificName"}, useInCompare=true) public String familyName;
		@FieldInfo(name="Subfamily", order=3.11, cols={"SUBFAMILYNAME", "MX.subfamily, MX.scientificName"}) public String subfamilyName;
		@FieldInfo(name="Tribe", order=3.12, cols={"TRIBENAME", "MX.tribe, MX.scientificName"}) public String tribeName;
		@FieldInfo(name="Subtribe", order=3.13, cols={"SUBTRIBENAME", "MX.subtribe, MX.scientificName"}) public String subtribeName;
		@FieldInfo(name="Genus", order=3.14, cols={"GENUSNAME", "MX.genus, MX.scientificName"}) public String genusName;
		@FieldInfo(name="Subgenus", order=3.15, cols={"SUBGENUSNAME", "MX.subgenus, MX.scientificName"}) public String subgenusName;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ChecklistRow other = (ChecklistRow) obj;
			if (author == null) {
				if (other.author != null)
					return false;
			} else if (!author.equals(other.author))
				return false;
			if (finnishName == null) {
				if (other.finnishName != null)
					return false;
			} else if (!finnishName.equals(other.finnishName))
				return false;
			if (scientificName == null) {
				if (other.scientificName != null)
					return false;
			} else if (!scientificName.equals(other.scientificName))
				return false;
			if (swedishName == null) {
				if (other.swedishName != null)
					return false;
			} else if (!swedishName.equals(other.swedishName))
				return false;
			if (given(this.familyName) && given(other.familyName)) { // note family comparison only if both have family 
				if (!this.familyName.equals(other.familyName))
					return false;
			}
			return true;
		}

		@Override
		public String toString() {
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

		@Override
		public int hashCode() {
			return toString().hashCode();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldInfo {
		public String name();
		public double order();
		public String[] cols();
		public boolean useInCompare() default false;
	}

	public static boolean given(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
