package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse.Match;

public class IUCN2010Sisaan {

	private static final String NOTES = "Notes";
	private static final int EVALUATION_YEAR = 2010;
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
		FILE_TO_INFORMAL_GROUP.put("Limasienet_siirto.csv", Utils.set(new Qname("MVL.321")));
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

	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAO taxonomyDAO;

	public static void main(String[] args) {
		DataSource dataSource = null;
		try {
			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));
			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO, new ErrorReporingToSystemErr());
			process();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		}
		System.out.println("done");
	}

	private static void process() throws Exception {
		File folder = new File("C:/esko-local/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/2010");
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
		Set<Qname> allowedInformalGroups = FILE_TO_INFORMAL_GROUP.get(f.getName());
		if (allowedInformalGroups == null) throw new IllegalStateException("No informal groups for " + f.getName());
		List<String> lines = FileUtils.readLines(f);
		int i = 0;
		for (String line : lines) {
			i++;
			line = line.trim();
			if (line.isEmpty()) continue;
			process(line, f, i, lines.size());
		}
	}

	private static void process(String line, File f, int i, int total) throws Exception {
		String[] parts = line.split(Pattern.quote("|"));
		IUCNLineData data = new IUCNLineData(parts);
		dump(data);
		process(data, f, i, total);
	}

	private static void process(IUCNLineData data, File f, int i, int total) {
		try {
			System.out.println(i + " / " + total + "\t" + data.getScientificName());
			TaxonSearchResponse response = taxonomyDAO.searchInternal(new TaxonSearch(data.getScientificName()).onlyExact());
			if (response.getExactMatches().isEmpty()) {
				response = taxonomyDAO.searchInternal(new TaxonSearch(data.getFinnishName()).onlyExact());
			}
			if (response.getExactMatches().isEmpty()) {
				for (String altname : data.getAlternativeFinnishNames()) {
					response = taxonomyDAO.searchInternal(new TaxonSearch(altname).onlyExact());
					if (!response.getExactMatches().isEmpty()) break;
				}
			}
			if (response.getExactMatches().isEmpty()) {
				reportTaxonNotFound("Tuntematon nimi", data, f);
				return;
			}
			Set<Qname> allowedInformalGroups = FILE_TO_INFORMAL_GROUP.get(f.getName());
			Set<Qname> matchingQnames = qnames(response.getExactMatches(), allowedInformalGroups);
			if (matchingQnames.size() == 1) {
				process(data, matchingQnames.iterator().next());
				return;
			}
			if (matchingQnames.size() > 1) {
				reportTaxonNotFound("Löytyi " + matchingQnames.size() + " osumaa lajiryhmistä " + allowedInformalGroups + ": " + matchingQnames, data, f);
				return;
			}
			StringBuilder message = new StringBuilder();
			message.append("Löytyi " + response.getExactMatches().size() + " osumaa, mutta väärästä lajiryhmästä: ");
			for (Match match : response.getExactMatches()) {
				message.append(match.getTaxonId()).append(" ").append(debug(match.getInformalGroups())).append(" ");
			}
			reportTaxonNotFound(message.toString().trim(), data, f);
		} catch (Exception e) {
			reportError(e, data, f);
		}	
	}

	private static String debug(List<InformalTaxonGroup> informalGroups) {
		StringBuilder b = new StringBuilder();
		for (InformalTaxonGroup i : informalGroups) {
			b.append(i.getName().forLocale("fi")).append(" ");
		}
		return b.toString().trim();
	}

	private static void process(IUCNLineData data, Qname taxonId) throws Exception {
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		IUCNEvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(taxonId.toString());
		if (target.hasEvaluation(EVALUATION_YEAR)) {
			System.out.println("\t\t\t\t\t\t\t\t\tSkipping ... already has evaluation for " + EVALUATION_YEAR);
			return; // Already loaded
		}
		IUCNEvaluation evaluation = toEvaluation(taxonId, data, EVALUATION_YEAR);
		iucnDAO.store(evaluation, null);
		iucnDAO.getIUCNContainer().setEvaluation(evaluation);
	}

	private static IUCNEvaluation toEvaluation(Qname taxonId, IUCNLineData data, int evaluationYear) throws Exception {
		IUCNEvaluation evaluation = taxonomyDAO.getIucnDAO().createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatementIfObjectGiven(IUCNEvaluation.IS_LOCKED, true);
		model.addStatementIfObjectGiven(IUCNEvaluation.STATE, new Qname(IUCNEvaluation.STATE_READY));
		model.addStatementIfObjectGiven(IUCNEvaluation.EVALUATED_TAXON, taxonId);
		model.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_YEAR, String.valueOf(evaluationYear));
		model.addStatementIfObjectGiven(IUCNEvaluation.BORDER_GAIN, data.getBorderGain());
		model.addStatementIfObjectGiven(IUCNEvaluation.BORDER_GAIN+NOTES, data.getBorderGainNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_A, data.getCriteriaA());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, data.getCriteriaB());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_C, data.getCriteriaC());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_D, data.getCriteriaD());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_E, data.getCriteriaE());
		model.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_FOR_STATUS, data.getCriteriaForStatus());
		model.addStatementIfObjectGiven(IUCNEvaluation.DECREASE_DURING_PERIOD+NOTES, data.getDecreaseDuringPeriodNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MAX, s(data.getDistributionAreaMax()));
		model.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MIN, s(data.getDistributionAreaMin()));
		model.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUATION_AREA_NOTES, data.getDistributionAreaNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, s(data.getEvaluationPeriodLength()));
		model.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH+NOTES, data.getEvaluationPeriodLengthNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.FRAGMENTED_HABITATS, data.getFragmentedHabitats());
		model.addStatementIfObjectGiven(IUCNEvaluation.FRAGMENTED_HABITATS+NOTES, data.getFragmentedHabitatsNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.GENERATION_AGE, s(data.getGenerationAge()));
		model.addStatementIfObjectGiven(IUCNEvaluation.GENERATION_AGE+NOTES, data.getGenerationAgeNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.GROUNDS_FOR_EVALUATION_NOTES, data.getGroundsForEvaluationNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.HABITAT_GENERAL_NOTES, data.getHabitatGeneralNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.HABITAT+NOTES, data.getHabitatNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.INDIVIDUAL_COUNT_MAX, s(data.getIndividualCountMax()));
		model.addStatementIfObjectGiven(IUCNEvaluation.INDIVIDUAL_COUNT_MIN, s(data.getIndividualCountMin()));
		model.addStatementIfObjectGiven(IUCNEvaluation.INDIVIDUAL_COUNT_NOTES, data.getIndividualCountNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.LAST_SIGHTING_NOTES, data.getLastSightingNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.LEGACY_PUBLICATIONS, data.getLegacyPublications());
		model.addStatementIfObjectGiven(IUCNEvaluation.LSA_RECOMMENDATION, data.getLsaRecommendation());
		model.addStatementIfObjectGiven(IUCNEvaluation.LSA_RECOMMENDATION+NOTES, data.getLsaRecommendationNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MAX, s(data.getOccurrenceAreaMax()));
		model.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MIN, s(data.getOccurrenceAreaMin()));
		model.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_NOTES, data.getOccurrenceAreaNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_NOTES, data.getOccurrenceNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_REGIONS_NOTES, data.getOccurrenceRegionsNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.POPULATION_SIZE_PERIOD_BEGINNING, s(data.getPopulationSizePeriodBeginning()));
		model.addStatementIfObjectGiven(IUCNEvaluation.POPULATION_SIZE_PERIOD_END, s(data.getPopulationSizePeriodEnd()));
		model.addStatementIfObjectGiven(IUCNEvaluation.POPULATION_SIZE_PERIOD_NOTES, data.getPopulationSizePeriodNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.POPULATION_VARIES, data.getPopulationVaries());
		model.addStatementIfObjectGiven(IUCNEvaluation.POPULATION_VARIES+NOTES, data.getPopulationVariesNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.POSSIBLY_RE, data.getPossiblyRE());
		model.addStatementIfObjectGiven(IUCNEvaluation.POSSIBLY_RE+NOTES, data.getPossiblyRENotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.REASON_FOR_STATUS_CHANGE+NOTES, data.getReasonForStatusChangeNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, data.getRedListStatus());
		model.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MAX, data.getRedListStatusMax());
		model.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MIN, data.getRedListStatusMin());
		model.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_NOTES, data.getRedListStatusNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.TAXONOMIC_NOTES, data.getTaxonomicNotes());
		model.addStatementIfObjectGiven(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, data.getTypeOfOccurrenceInFinland());
		model.addStatementIfObjectGiven(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND+NOTES, data.getTypeOfOccurrenceInFinlandNotes());
		int i = 0;
		for (Qname q : data.getEndangermentReasons()) {
			evaluation.addEndangermentReason(new IUCNEndangermentObject(null, q, i++));
		}
		i = 0;
		for (Qname q : data.getThreats()) {
			evaluation.addThreat(new IUCNEndangermentObject(null, q, i++));
		}
		for (Map.Entry<Qname, Qname> e : data.getOccurrences().entrySet()) {
			Qname area = e.getKey();
			Qname status = e.getValue();
			evaluation.addOccurrence(new Occurrence(null, area, status));
		}
		evaluation.setPrimaryHabitat(data.getPrimaryHabitat());
		for (IUCNHabitatObject habitat : data.getSecondaryHabitats()) {
			evaluation.addSecondaryHabitat(habitat);
		}
		for (Qname q : data.getReasonForStatusChange()) {
			model.addStatementIfObjectGiven(IUCNEvaluation.REASON_FOR_STATUS_CHANGE, q);
		}
		return evaluation;
	}

	private static String s(Double d) {
		if (d == null) return null;
		return d.toString();
	}

	private static String s(Integer i) {
		if (i == null) return null;
		return i.toString();
	}

	private static Set<Qname> qnames(List<Match> exactMatches, Set<Qname> allowedInformalGroups) {
		Set<Qname> set = new HashSet<>();
		for (Match m : exactMatches) {
			for (InformalTaxonGroup i : m.getInformalGroups()) {
				if (allowedInformalGroups.contains(i.getQname())) {
					set.add(m.getTaxonId());
				}
			}
		}
		return set;
	}

	private static void reportTaxonNotFound(String message, IUCNLineData data, File f) {
		File file = new File("c:/temp/iucn/missing_" + f.getName().replace(".csv", ".txt"));
		try {
			StringBuilder b = new StringBuilder();
			b.append(data.getScientificName()).append("|");
			b.append(data.getFinnishName()).append("|");
			if (!data.getAlternativeFinnishNames().isEmpty()) b.append(data.getAlternativeFinnishNames().toString());
			b.append("|");
			b.append(message);
			FileUtils.writeToFile(file, b.toString()+"\n", true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static void reportError(Exception e, IUCNLineData data, File f) {
		File errorFile = new File("c:/temp/iucn/error_" + f.getName().replace(".csv", ".txt"));
		try {
			FileUtils.writeToFile(errorFile, data.getScientificName() + ":\n" + LogUtils.buildStackTrace(e, 5)+"\n", true);
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
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
