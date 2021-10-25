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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;

public class ChecklistComparison {

	private static final String PREV_YEAR = "2020";
	private static final String LATEST_YEAR = "2021";

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

	private static class Comparison {
		StringBuilder human = new StringBuilder();
		StringBuilder machine = new StringBuilder();
		StringBuilder deleted = new StringBuilder();
	}

	private static void compareChecklists() throws Exception {
		File latest = new File("E:/esko-local/temp/checklist2021/2021.tsv");
		File previous = new File("E:/esko-local/temp/checklist2021/2020.tsv");

		Comparison c = new ChecklistComparator().compare(
				new ChecklistReader().read(latest),
				new ChecklistReader().read(previous));

		FileUtils.writeToFile(new File("E:/esko-local/temp/checklist2021/checklist_comparison_"+DateUtils.getFilenameDatetime()+".tsv"), c.human.toString());
		FileUtils.writeToFile(new File("E:/esko-local/temp/checklist2021/checklist_diff_"+DateUtils.getFilenameDatetime()+".tsv"), c.machine.toString());
		FileUtils.writeToFile(new File("E:/esko-local/temp/checklist2021/checklist_removed_"+DateUtils.getFilenameDatetime()+".tsv"), c.deleted.toString());
	}

	private static class Difference {
		String field;
		String oldValue;
		String newValue;
		public Difference(String field, String oldValue, String newValue) {
			this.field = field;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}

	private static class ChecklistComparator {

		public Comparison compare(Checklist latest, Checklist previous) {
			Comparison c = new Comparison();
			colHeadersHuman(c.human);
			colHeadersMachine(c.machine);
			latest.rows.values().forEach(row -> {
				compare(previous, row, c);
			});

			colHeadersHuman(c.deleted);
			previous.rows.values().forEach(prevRow -> {
				if (!latest.contains(prevRow.taxonId)) {
					c.deleted.append(PREV_YEAR).append(TAB);
					c.deleted.append(prevRow.toString()).append(NEWLINE);
				}
			});

			return c;
		}

		private void compare(Checklist previous, ChecklistRow row, Comparison c) {
			ChecklistRow prevRow = previous.getRow(row.taxonId);
			compareMachine(row, prevRow, c);
			compareHuman(row, prevRow, c);
		}



		private void compareHuman(ChecklistRow row, ChecklistRow prevRow, Comparison c) {
			if (prevRow == null) {
				c.human.append(LATEST_YEAR).append(TAB);
				c.human.append(row.toString());
				c.human.append(NEWLINE);
				c.human.append("NEW TAXON");
				c.human.append(NEWLINE).append(NEWLINE);
				return;
			}
			List<Difference> differences = differences(row, prevRow, false);
			if (!differences.isEmpty()) {
				c.human.append(LATEST_YEAR).append(TAB);
				c.human.append(row.toString());
				c.human.append(NEWLINE);
				c.human.append(PREV_YEAR).append(TAB);
				c.human.append(prevRow.toString()).append(NEWLINE);
				for (Difference d : differences) {
					c.human.append(d.field).append(": ").append(d.oldValue).append(" -> ").append(d.newValue).append(NEWLINE);
				}
				c.human.append(NEWLINE);
			}
		}

		private void compareMachine(ChecklistRow row, ChecklistRow prevRow, Comparison c) {
			List<Difference> differences = differences(row, prevRow, true);
			for (Difference d : differences) {
				c.machine.append(row.taxonId).append(TAB).append(d.field).append(TAB).append(d.oldValue).append(TAB).append(d.newValue).append(NEWLINE);
			}
		}

		private List<Difference> differences(ChecklistRow latest, ChecklistRow prevRow, boolean all) {
			List<Difference> differences = new ArrayList<>();
			for (Field f : FIELDS.keySet()) {
				if (!all && !f.getAnnotation(FieldInfo.class).useInCompare()) continue;
				try {
					String valueOfLatest = (String) f.get(latest);
					String valueOfPrev = prevRow == null ? "" : (String) f.get(prevRow);
					if (valueOfLatest == null) valueOfLatest = "";
					if (valueOfPrev == null) valueOfPrev = "";
					if (!valueOfLatest.equals(valueOfPrev)) {
						String colname = colName(f);
						differences.add(new Difference(colname, valueOfPrev, valueOfLatest));
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return differences;
		}
		private void colHeadersHuman(StringBuilder b) {
			b.append("Year/Changes").append(TAB);
			for (Field f : FIELDS.keySet()) {
				b.append(colName(f)).append(TAB);
			}
			b.append(NEWLINE);
		}

		private void colHeadersMachine(StringBuilder b) {
			b.append("id").append(TAB).append("field").append(TAB).append("old").append(TAB).append("new").append(NEWLINE);

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
				f.set(row, value.replace("http://tun.fi/", ""));
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
		boolean contains(String taxonId) {
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
		@FieldInfo(name="Id", order=0, cols={"Identifier"}) public String taxonId;
		@FieldInfo(name="Scientific name", order=1.1, cols={"Scientific name"}, useInCompare=true) public String scientificName;
		@FieldInfo(name="Author", order=1.2, cols={"Author"}, useInCompare=true) public String author;
		@FieldInfo(name="Finnish name", order=2.1, cols={"Finnish name"}, useInCompare=true) public String finnishName;
		@FieldInfo(name="Swedish name", order=2.2, cols={"Swedish name"}, useInCompare=true) public String swedishName;
		@FieldInfo(name="Domain", order=3.01, cols={"Domain", "Domain, Scientific name"}) public String domainName;
		@FieldInfo(name="Kingdom", order=3.02, cols={"Kingdom", "Kingdom, Scientific name"}) public String kingdomName;
		@FieldInfo(name="Phylum", order=3.031, cols={"Phylum", "Phylum, Scientific name"}) public String phylumName;
		@FieldInfo(name="Division", order=3.04, cols={"Division", "Division, Scientific name"}) public String divisionName;
		@FieldInfo(name="Class", order=3.05, cols={"Class", "Class, Scientific name"}) public String className;
		@FieldInfo(name="Subclass", order=3.06, cols={"Subclass", "Subclass, Scientific name"}) public String subclassName;
		@FieldInfo(name="Order", order=3.07, cols={"Order", "Order, Scientific name"}) public String orderName;
		@FieldInfo(name="Suborder", order=3.08, cols={"Suborder", "Suborder, Scientific name"}) public String suborderName;
		@FieldInfo(name="Superfamily", order=3.09, cols={"Superfamily", "Superfamily, Scientific name"}) public String superfamilyName;
		@FieldInfo(name="Family", order=3.10, cols={"Family", "Family, Scientific name"}, useInCompare=true) public String familyName;
		@FieldInfo(name="Subfamily", order=3.11, cols={"Subfamily", "Subfamily, Scientific name"}) public String subfamilyName;
		@FieldInfo(name="Tribe", order=3.12, cols={"Tribe", "Tribe, Scientific name"}) public String tribeName;
		@FieldInfo(name="Subtribe", order=3.13, cols={"Subtribe", "Subtribe, Scientific name"}) public String subtribeName;
		@FieldInfo(name="Genus", order=3.14, cols={"Genus", "Genus, Scientific name"}) public String genusName;
		@FieldInfo(name="Subgenus", order=3.15, cols={"Subgenus", "Subgenus, Scientific name"}) public String subgenusName;

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
