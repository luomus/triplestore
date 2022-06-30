package fi.luomus.triplestore.taxonomy.runnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;
import fi.luomus.triplestore.taxonomy.models.TaxonomyDAOStub;

public class TaxaCreationFromFile {

	private static final String FILENAME_IN = "E:\\esko-local\\temp\\kalat.txt";
	private static final String FILENAME_OUT = "E:\\esko-local\\temp\\taxon_statements.txt";
	private static final boolean DRY_RUN = true;


	private static final List<String> warnings = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println("running");
		try {
			create();
			if (warnings.isEmpty()) {
				System.out.println("end - success");
			}
			System.out.println("end - with warnings:");
			warnings.forEach(w->System.out.println(w));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("end - fail");
		}
	}

	private static void create() throws Exception {
		List<String> lines = FileUtils.readLines(new File(FILENAME_IN));
		Iterator<String> i = lines.iterator();
		List<String> headers = headers(i.next());
		List<Map<String, String>> data = data(headers, i);
		List<Taxon> taxa = taxa(data);
		resolveParents(taxa);
		for (Taxon t : taxa) {
			debug(t);
		}
	}

	private static void debug(Taxon t) {
		Utils.debug(t.getId(), t.getParentQname(), t.getTaxonRank(), t.getScientificName(), t.getScientificNameAuthorship(), t.getVernacularName().getAllTexts(), t.getAlternativeVernacularNames().getAllTexts());
	}

	private static void resolveParents(List<Taxon> taxa) {
		// TODO Auto-generated method stub
		Qname parentId = null;
		for (Taxon t : taxa) {

		}
	}



	private static List<Taxon> taxa(List<Map<String, String>> data) {
		List<Taxon> taxa = new ArrayList<>();
		for (Map<String, String> d : data) {
			taxa.add(createTaxon(d));
		}
		return taxa;
	}

	private static Taxon createTaxon(Map<String, String> data) {
		String id = data.get("TaxonID");
		Taxon taxon = given(id) ? new Taxon(new Qname(id), null) : new Taxon(new Qname(Utils.generateGUID()), null);
		for (String field : data.keySet()) {
			if (field.startsWith("#")) continue;
			if (field.equals("TaxonID")) continue;
			String value = data.get(field);
			if (field.equals("TaxonRank")) {
				taxon.setTaxonRank(parseTaxonRank(value));
				continue;
			} else if (field.equals("ScientificName")) {
				taxon.setScientificName(value);
				continue;
			} else if (field.equals("Author")) {
				taxon.setScientificNameAuthorship(value);
				continue;
			} else if (field.equals("VernacularFI")) {
				taxon.addVernacularName("fi", value);
				continue;
			} else if (field.equals("AlternativeFI")) {
				taxon.addAlternativeVernacularName("fi", value);
				continue;
			} else if (field.equals("VernacularEN")) {
				taxon.addVernacularName("en", Utils.upperCaseFirst(value));
				continue;
			} else if (field.equals("AlternativeEN")) {
				taxon.addAlternativeVernacularName("en", Utils.upperCaseFirst(value));
				continue;
			}
			throw new IllegalStateException("Unknown field " + field);
		}
		validate(taxon, data);
		return taxon;
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
		RANK_MAP.put("lahko", new Qname("MX.order"));
		RANK_MAP.put("alalahko", new Qname("MX.suborder"));
		RANK_MAP.put("heimo", new Qname("MX.family"));
		RANK_MAP.put("alaheimo", new Qname("MX.subfamily"));
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
			headers.add(s.trim());
		}
		return headers;
	}

	private static List<Map<String, String>> data(List<String> headers, Iterator<String> lines) {
		List<Map<String, String>> data = new ArrayList<>();
		while (lines.hasNext()) {
			String line = lines.next();
			if (Utils.removeWhitespace(line).isEmpty()) continue;
			Iterator<String> header = headers.iterator();
			Map<String, String> map = new LinkedHashMap<>();
			for (String s : line.split(Pattern.quote("\t"))) {
				map.put(header.next(), s.trim());
			}
			data.add(map);
		}
		return data;
	}

	private static boolean given(Object o) {
		return o != null && !o.toString().isEmpty();
	}

}
