package fi.luomus.triplestore.taxonomy.runnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.methods.HttpGet;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;
import fi.luomus.triplestore.taxonomy.models.TaxonomyDAOStub;

public class TaxaCreationFromFile {

	private static final String API_URL = "https://triplestore.luomus.fi/uri/MX";
	private static final String API_USERNAME = "...";
	private static final String API_PASSWORD = "...";

	// Note: Example data file can be found from /data folder of this repo
	private static final String FILENAME_IN = "E:\\esko-local\\git\\eskon-dokkarit\\data\\taxonomy\\Maailman kalat tietokantaan_2025_TAXONID.txt";
	private static final String FILENAME_OUT = "E:\\esko-local\\git\\eskon-dokkarit\\data\\taxonomy\\maailman_kalat_taxon_statements_"+DateUtils.getFilenameDatetime()+".txt";
	private static final boolean DRY_RUN = true; // XXX

	private static final String CREATED_TIMESTAMP = Long.toString(DateUtils.getCurrentEpoch()); // Note: All created new taxa have the same created timestamp making it easy to identify them - to fix/undo things gone wrong...

	private static int seq = 1;
	private static final List<String> warnings = new ArrayList<>();

	private static final String MZ_CREATED_AT_TIMESTAMP = "MZ.createdAtTimestamp";
	private static final String MX_IS_PART_OF = "MX.isPartOf";
	private static final String MX_VERCACULAR_NAME = "MX.vernacularName";
	private static final String MX_ALTERNATIVE_VERNACULAR_NAME = "MX.alternativeVernacularName";
	private static final String MX_OBSOLETE_VERNACULAR_NAME = "MX.obsoleteVernacularName";
	private static final String MX_NAME_ACCORDING_TO = "MX.nameAccordingTo";
	private static final String MX_TAXON_RANK = "MX.taxonRank";
	private static final String MX_SCIENTIFIC_NAME_AUTHORSHIP = "MX.scientificNameAuthorship";
	private static final String MX_SCIENTIFIC_NAME = "MX.scientificName";
	private static final String MX_TAXON = "MX.taxon";
	private static final String MX_IS_PART_OF_INFORMAL_GROUP = "MX.isPartOfInformalTaxonGroup";

	public static void main(String[] args) {
		System.out.println("running");
		try {
			create();
			if (warnings.isEmpty()) {
				System.out.println("end - success");
			} else {
				System.out.println("end - with warnings:");
				warnings.forEach(w->System.out.println(w));
			}

			// Or maybe add old names (manually?) as alternative names?
			System.out.println("Remember to run the following: ");
			System.out.println("--delete from rdf_statement where statementid in ( ");
			System.out.println("	select subjectname, langcodefk, count(1), min(statementid) ");
			System.out.println("	from s ");
			System.out.println("	where predicatename = 'MX.vernacularName' ");
			System.out.println("	group by subjectname, langcodefk ");
			System.out.println("	having count(1) > 1 ");
			System.out.println("); ");
			System.out.println("exit");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("end - fail");
		}
	}

	private static void create() throws Exception {
		List<Map<String, String>> data = readData();
		List<Taxon> taxa = createTaxa(data);
		for (Taxon t : taxa) {
			debug(t);
		}
		List<String> statements = createStatements(taxa);
		if (!DRY_RUN) {
			replaceTempIdsWithReal(statements);
		}
		for (String s : statements) {
			System.out.println(s);
		}
		FileUtils.writeToFile(new File(FILENAME_OUT), statements.stream().collect(Collectors.joining("\n")));
	}

	private static void replaceTempIdsWithReal(List<String> statements) {
		int maxUsed = seq;
		Map<String, String> replace = new HashMap<>();
		for (int i = 1; i<=maxUsed; i++) {
			String id = getNextRealSeqValue();
			replace.put("TEMP."+i, id);
		}
		for (Map.Entry<String, String> e : replace.entrySet()) {
			String temp = e.getKey();
			String real = e.getValue();
			ListIterator<String> i = statements.listIterator();
			while (i.hasNext()) {
				i.set(i.next().replace("'"+temp+"'", "'"+real+"'"));
			}
		}
	}

	private static String getNextRealSeqValue() {
		try (HttpClientService client = new HttpClientService(API_URL, API_USERNAME, API_PASSWORD)) {
			JSONObject json = client.contentAsJson(new HttpGet(API_URL));
			return json.getObject("response").getString("qname");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static List<String> createStatements(List<Taxon> taxa) {
		List<String> statements = new ArrayList<>();
		statements.add("set define off;"); // This fixes & character causing a replacement dialog
		for (Taxon taxon : taxa) {
			if (isExisting(taxon)) {
				Model model = new Model(taxon.getId());
				if (taxon.getParentQname() != null && !isExisting(taxon.getParentQname())) {
					statements.add("DELETE FROM s WHERE subjectname = '"+taxon.getId()+"' AND predicatename = 'MX.isPartOf';");
					model.addStatementIfObjectGiven(MX_IS_PART_OF, taxon.getParentQname());
				}
				for (Qname groupId : taxon.getExplicitlySetInformalTaxonGroups()) {
					model.addStatementIfObjectGiven(MX_IS_PART_OF_INFORMAL_GROUP, groupId);
				}
				vernacularNameStatements(taxon, model);
				statements.addAll(generateStatements(model));
			} else {
				statements.add("EXEC AddResource('"+taxon.getId()+"');");
				Model model = new Model(taxon.getId());
				model.setType(MX_TAXON);
				model.addStatementIfObjectGiven(MX_SCIENTIFIC_NAME, taxon.getScientificName());
				model.addStatementIfObjectGiven(MX_SCIENTIFIC_NAME_AUTHORSHIP, taxon.getScientificNameAuthorship());
				model.addStatementIfObjectGiven(MX_TAXON_RANK, taxon.getTaxonRank());
				model.addStatementIfObjectGiven(MX_NAME_ACCORDING_TO, taxon.getChecklist());
				model.addStatementIfObjectGiven(MX_IS_PART_OF, taxon.getParentQname());
				for (Qname groupId : taxon.getExplicitlySetInformalTaxonGroups()) {
					model.addStatementIfObjectGiven(MX_IS_PART_OF_INFORMAL_GROUP, groupId);
				}
				vernacularNameStatements(taxon, model);
				model.addStatement(new Statement(new Predicate(MZ_CREATED_AT_TIMESTAMP), new ObjectLiteral(CREATED_TIMESTAMP)));
				statements.addAll(generateStatements(model));
			}
		}
		return statements;
	}

	private static Collection<String> generateStatements(Model model) {
		if (model.isEmpty()) return Collections.emptyList();
		List<String> statements = new ArrayList<>();
		for (Statement s : model.getStatements()) {
			if (s.isLiteralStatement()) {
				if (s.getObjectLiteral().hasLangcode()) {
					statements.add("EXEC AddStatementL('"+model.getSubject().getQname()+"', '"+s.getPredicate().getQname()+"', '"+s.getObjectLiteral().getContent().replace("'", "''")+"', '"+s.getObjectLiteral().getLangcode()+"');");
				} else {
					statements.add("EXEC AddStatementL('"+model.getSubject().getQname()+"', '"+s.getPredicate().getQname()+"', '"+s.getObjectLiteral().getContent().replace("'", "''")+"');");
				}
			} else {
				statements.add("EXEC AddStatement('"+model.getSubject().getQname()+"', '"+s.getPredicate().getQname()+"', '"+s.getObjectResource().getQname()+"');");
			}
		}
		return statements;
	}

	private static void vernacularNameStatements(Taxon taxon, Model model) {
		for (Map.Entry<String, String> e : taxon.getVernacularName().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(MX_VERCACULAR_NAME, e.getValue(), e.getKey());
		}
		for (Map.Entry<String, List<String>> e : taxon.getAlternativeVernacularNames().getAllTexts().entrySet()) {
			for (String s : e.getValue()) {
				model.addStatementIfObjectGiven(MX_ALTERNATIVE_VERNACULAR_NAME, s, e.getKey());
			}
		}
		for (Map.Entry<String, List<String>> e : taxon.getObsoleteVernacularNames().getAllTexts().entrySet()) {
			for (String s : e.getValue()) {
				model.addStatementIfObjectGiven(MX_OBSOLETE_VERNACULAR_NAME, s, e.getKey());
			}
		}
	}

	private static boolean isExisting(Taxon t) {
		return isExisting(t.getId());
	}

	private static boolean isExisting(Qname id) {
		return id.toString().startsWith("MX.");
	}

	private static List<Taxon> createTaxa(List<Map<String, String>> data) {
		List<Taxon> taxa = taxa(data);
		resolveParents(taxa);
		return taxa;
	}

	private static List<Map<String, String>> readData() throws FileNotFoundException, IOException {
		List<String> lines = FileUtils.readLines(new File(FILENAME_IN));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			line = line.replace("\u00A0", " ");
			lines.set(i, line);
		}
		Iterator<String> i = lines.iterator();
		List<String> headers = headers(i.next());
		List<Map<String, String>> data = parseData(headers, i);
		return data;
	}

	private static void debug(Taxon t) {
		List<Object> s = Utils.list(TaxonRank.fromTaxon(t).indent(), t.getId(), t.getParentQname(), t.getTaxonRank(), t.getScientificName(), t.getScientificNameAuthorship(), t.getVernacularName().getAllTexts(), t.getAlternativeVernacularNames().getAllTexts(), t.getExplicitlySetInformalTaxonGroups());
		String debug = s.stream().map(o->o==null?"":o.toString()).collect(Collectors.joining("\t"));
		System.out.println(debug);
	}

	private static void resolveParents(List<Taxon> taxa) {
		Map<Integer, Qname> parentChain = new TreeMap<>();
		for (Taxon t : taxa) {
			Qname parent = resolveParent(parentChain, t);
			t.setParentQname(parent);
			updateChain(parentChain, t);
		}
	}

	private static void updateChain(Map<Integer, Qname> parentChain, Taxon t) {
		int order = getOrder(t);
		parentChain.put(order, t.getId());
		for (int i = order+1; i<50; i++) {
			parentChain.remove(i);
		}
	}

	private static Qname resolveParent(Map<Integer, Qname> parentChain, Taxon t) {
		int order = getOrder(t);
		for (int i = order-1; i > 0; i--) {
			if (parentChain.containsKey(i)) return parentChain.get(i);
		}
		return null;
	}

	private static int getOrder(Taxon t) {
		int order = TaxonRank.fromTaxon(t).order;
		return order;
	}

	private static List<Taxon> taxa(List<Map<String, String>> data) {
		List<Taxon> taxa = new ArrayList<>();
		for (Map<String, String> d : data) {
			createTaxon(d).stream().filter(t->t!=null).forEach(taxa::add);
		}
		return taxa;
	}

	private static Taxon prevTaxon = null;

	private static Collection<Taxon> createTaxon(Map<String, String> data) {
		Taxon taxon = createNewTaxon(resolveTaxonId(data.get("taxonid")));
		Taxon species = null;

		for (String field : data.keySet()) {
			field = field.toLowerCase().trim();
			if (field.startsWith("#")) continue;
			if (field.equals("taxonid")) continue;
			if (field.endsWith("speciesid")) continue;
			String value = data.get(field);
			if (!given(value)) continue;
			value = Utils.removeWhitespaceAround(value);
			if (!given(value)) continue;
			if (field.equals("taxonrank") || field.equals("rank")) {
				taxon.setTaxonRank(parseTaxonRank(value));
				continue;
			} else if (field.equals("scientificname") || field.equals("taxon")) {
				taxon.setScientificName(value);
				continue;
			} else if (field.equals("author")) {
				taxon.setScientificNameAuthorship(value);
				continue;
			} else if (field.equals("vernacularfi")) {
				taxon(taxon, species).addVernacularName("fi", value);
				continue;
			} else if (field.equals("alternativefi")) {
				for (String s : multi(value)) {
					taxon(taxon, species).addAlternativeVernacularName("fi", s);
				}
				continue;
			} else if (field.equals("vernacularen")) {
				taxon(taxon, species).addVernacularName("en", Utils.upperCaseFirst(value));
				continue;
			} else if (field.equals("alternativeen")) {
				for (String s : multi(value)) {
					taxon(taxon, species).addAlternativeVernacularName("en", Utils.upperCaseFirst(s));
				}
				continue;
			} else if (field.equals("obsoletevernacularnamefi") || field.equals("obsoletefi")) {
				for (String s : multi(value)) {
					taxon(taxon, species).addObsoleteVernacularName("fi", s);
				}
				continue;
			} else if (field.equals("speciesscientificname")) {
				Qname speciesId = resolveTaxonId(data.get("speciesid"));
				species = createNewTaxon(speciesId);
				species.setTaxonRank(new Qname("MX.species"));
				species.setScientificName(value);
				continue;
			} else if (field.equals("speciesauthor")) {
				if (species == null) throw new IllegalStateException("Species author without species name: " + data);
				species.setScientificNameAuthorship(value);
				continue;
			} else if (field.equals("informalgroup")) {
				if (value.contains(",")) {
					for (String id : value.split(Pattern.quote(","))) {
						taxon(taxon, species).addInformalTaxonGroup(new Qname(id.trim()));
					}
				} else {
					taxon(taxon, species).addInformalTaxonGroup(new Qname(value.trim()));
				}
				continue;
			}
			throw new IllegalStateException("Unknown field " + field);
		}
		if (species == null && taxon.getTaxonRank() == null) {
			taxon.setTaxonRank(new Qname("MX.species"));
		}
		validate(taxon, data);
		if (prevTaxon != null && prevTaxon.getScientificName().equals(taxon.getScientificName())) {
			taxon = null;
		} else {
			prevTaxon = taxon;
		}
		if (species != null) {
			validate(species, data);
		}
		return Utils.list(taxon, species);
	}

	private static List<String> multi(String value) {
		List<String> values = new ArrayList<>();
		for (String s : value.split(Pattern.quote(";") )) {
			values.add(s.trim());
		}
		return values;
	}

	private static Taxon taxon(Taxon taxon, Taxon species) {
		return species == null ? taxon : species;
	}

	private static Taxon createNewTaxon(Qname taxonId) {
		Taxon taxon = new Taxon(taxonId, null);
		taxon.setChecklist(new Qname("MR.1"));
		return taxon;
	}

	private static Qname resolveTaxonId(String id) {
		if (given(id)) {
			Qname taxonId = new Qname(id.trim());
			validateTaxonId(taxonId);
			return taxonId;
		}
		return new Qname(nextId());
	}

	private static void validateTaxonId(Qname taxonId) {
		if (!taxonId.toString().startsWith("MX.")) throw new IllegalStateException("Invalid taxon id " + taxonId);
		try {
			String s = taxonId.toString().replaceFirst("MX.", "");
			if (!s.equals(Utils.removeWhitespace(s))) throw new IllegalStateException("Invalid taxon id " + taxonId);
			Integer.valueOf(s);
		} catch (Exception e) {
			throw new IllegalStateException("Invalid taxon id " + taxonId);
		}
	}

	private static String nextId() {
		return "TEMP." + (seq++);
	}

	private static void validate(Taxon taxon, Map<String, String> originalData) {
		if (!given(taxon.getScientificName())) throw new IllegalStateException("No scientificname for data row " + originalData);
		if (!given(taxon.getTaxonRank())) throw new IllegalStateException("No taxon rank for data row " + originalData);

		StringBuilder errors = new StringBuilder();
		ErrorReporter errorReporter = new ErrorReporter() {
			@Override
			public void report(String message, Throwable condition) {
				errors.append(message + LogUtils.buildStackTrace(condition));
			}

			@Override
			public void report(String message) {
				errors.append(message);
			}

			@Override
			public void report(Throwable condition) {
				errors.append(LogUtils.buildStackTrace(condition));
			}
		};

		ExtendedTaxonomyDAO dao = new FakeDAO();

		TaxonValidator validator = new TaxonValidator(null, dao, errorReporter);
		ValidationData validationData = validator.validate(taxon);
		if (errors.length() > 0) {
			throw new IllegalStateException("Running validation caused exception: " + errors.toString() + " for data " + originalData);
		}
		if (validationData.hasErrors()) {
			throw new IllegalStateException("Validation error:" + validationData.getErrors() + " for data " + originalData);
		}
		if (validationData.hasWarnings()) {
			warnings.add("Validation warning " + validationData.getWarnings() + " for data " + originalData);
		}
	}

	private static class FakeDAO extends TaxonomyDAOStub {
		@Override
		public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Taxon taxon) {
			return Collections.emptyList();
		}
	}

	private static final Map<String, Qname> RANK_MAP;
	static {
		RANK_MAP = new HashMap<>();
		RANK_MAP.put("pääjakso", new Qname("MX.phylum"));
		RANK_MAP.put("alajakso", new Qname("MX.subphylum"));
		RANK_MAP.put("yläluokka", new Qname("MX.superclass"));
		RANK_MAP.put("luokka", new Qname("MX.class"));
		RANK_MAP.put("alaluokka", new Qname("MX.subclass"));
		RANK_MAP.put("lahko", new Qname("MX.order"));
		RANK_MAP.put("order", new Qname("MX.order"));
		RANK_MAP.put("alalahko", new Qname("MX.suborder"));
		RANK_MAP.put("osalahko", new Qname("MX.infraorder"));
		RANK_MAP.put("yläheimo", new Qname("MX.superfamily"));
		RANK_MAP.put("heimo", new Qname("MX.family"));
		RANK_MAP.put("alaheimo", new Qname("MX.subfamily"));
		RANK_MAP.put("tribe", new Qname("MX.tribe"));
		RANK_MAP.put("tribus", new Qname("MX.tribe"));
		RANK_MAP.put("sukuryhmä", new Qname("MX.tribe"));
		RANK_MAP.put("suku", new Qname("MX.genus"));
	}

	private static Qname parseTaxonRank(String value) {
		if (!given(value)) return null;
		Qname rank = RANK_MAP.get(value);
		if (rank == null) throw new IllegalStateException("Unknown taxon rank " + value);
		return rank;
	}

	private static List<String> headers(String line) {
		List<String> headers = new ArrayList<>();
		for (String s : line.split(Pattern.quote("\t"))) {
			headers.add(s.trim().toLowerCase());
		}
		return headers;
	}

	private static List<Map<String, String>> parseData(List<String> headers, Iterator<String> lines) {
		List<Map<String, String>> data = new ArrayList<>();
		while (lines.hasNext()) {
			String line = lines.next();
			if (Utils.removeWhitespace(line).isEmpty()) continue;
			Iterator<String> header = headers.iterator();
			Map<String, String> map = new LinkedHashMap<>();
			for (String s : line.split(Pattern.quote("\t"), -1)) {
				if (header.hasNext()) {
					map.put(header.next(), s.trim());
				}
			}
			data.add(map);
		}
		return data;
	}

	private static boolean given(Object o) {
		return o != null && !o.toString().isEmpty();
	}

	enum TaxonRank {
		SUPERDOMAIN(0, "MX.superdomain"),
		DOMAIN(1, "MX.domain"),

		KINGDOM(2, "MX.kingdom"),
		SUBKINGDOM(3, "MX.subkingdom"),
		INFRAKINGDOM(4, "MX.infrakingdom"),

		SUPERPHULYM(5, "MX.superphylum"),
		PHULYM(6, "MX.phylum"),
		SUBPHYLUM(7, "MX.subphylum"),
		INFRAPHYLUM(8, "MX.infraphylum"),

		SUPERCLASS(9, "MX.superclass"),
		CLASS(10, "MX.class"),
		SUBCLASS(11, "MX.subclass"),

		INFRACLASS(12, "MX.infraclass"),
		PARVCLASS(13, "MX.parvclass"),

		SUPERORDER(14, "MX.superorder"),
		ORDER(15, "MX.order"),
		SUBORDER(16, "MX.suborder"),
		INFRAORDER(17, "MX.infraorder"),
		PARVORDER(18, "MX.parvorder"),

		SUPERFAMILY(19, "MX.superfamily"),
		FAMINLY(20, "MX.family"),
		SUBFAMILY(21, "MX.subfamily"),

		TRIBE(22, "MX.tribe"),
		SUBTRIBE(23, "MX.subtribe"),

		SUPERGENUS(24, "MX.supergenus"),
		GENUS(25, "MX.genus"),
		NOTHOGENUS(26, "MX.nothogenus"),
		SUBGENUS(27, "MX.subgenus"),

		SECTION(28, "MX.section"),
		SUBSECTION(29, "MX.subsection"),
		SERIES(30, "MX.series"),
		SUBSERIES(31, "MX.subseries"),

		SPECIES(32, "MX.species");

		private final int order;
		private final Qname qname;

		private TaxonRank(int order, String qname) {
			this.order = order;
			this.qname = new Qname(qname);
		}

		public String indent() {
			String s = "                                                             ";
			return s.substring(0, order);
		}

		public static TaxonRank fromTaxon(Taxon t) {
			for (TaxonRank r : TaxonRank.values()) {
				if (r.qname.equals(t.getTaxonRank())) return r;
			}
			throw new IllegalArgumentException("Unknown rank " + t.getTaxonRank());
		}
	}

}
