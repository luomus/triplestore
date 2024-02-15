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

import com.zaxxer.hikari.HikariDataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReportingToSystemErr;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonSearchResponse.Match;
import fi.luomus.commons.taxonomy.iucn.EndangermentObject;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.runnable.IUCNLineData.Mode;

public class IUCN2010Sisaan {

	private static final Qname MISAPPLIED = new Qname("MX.hasMisappliedName");
	private static final String FILE_PATH = "C:/esko-local/workspace/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/";
	private static final String NOTES = "Notes";
	private static final int EVALUATION_YEAR = 2010; // XXX change year here
	private static final String OCCURRENCES = "occurrences";
	private static final Map<String, Set<Qname>> FILE_TO_INFORMAL_GROUP;
	static {
		FILE_TO_INFORMAL_GROUP = new HashMap<>();
		FILE_TO_INFORMAL_GROUP.put("Helttasienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		FILE_TO_INFORMAL_GROUP.put("Hämähäkit_siirto.csv", Utils.set(new Qname("MVL.38")));
		FILE_TO_INFORMAL_GROUP.put("Hamahakit_loput.csv", Utils.set(new Qname("MVL.38")));
		FILE_TO_INFORMAL_GROUP.put("Jäkälät_siirto.csv", Utils.set(new Qname("MVL.25")));
		FILE_TO_INFORMAL_GROUP.put("Jäkälät_korjattavat.csv", Utils.set(new Qname("MVL.25")));
		FILE_TO_INFORMAL_GROUP.put("Jäytiäiset_siirto.csv", Utils.set(new Qname("MVL.227")));
		FILE_TO_INFORMAL_GROUP.put("Kalat_siirto.csv", Utils.set(new Qname("MVL.27")));
		FILE_TO_INFORMAL_GROUP.put("Kierresiipiset_siirto.csv", Utils.set(new Qname("MVL.229")));
		FILE_TO_INFORMAL_GROUP.put("Kolmisukahäntäiset_siirto.csv", Utils.set(new Qname("MVL.301")));
		FILE_TO_INFORMAL_GROUP.put("Korennot_siirto.csv", Utils.set(new Qname("MVL.36"), new Qname("MVL.222")));
		FILE_TO_INFORMAL_GROUP.put("Kotelosienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		FILE_TO_INFORMAL_GROUP.put("Kovakuoriaiset_siirto.csv", Utils.set(new Qname("MVL.33")));
		FILE_TO_INFORMAL_GROUP.put("Kupusienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		FILE_TO_INFORMAL_GROUP.put("Kärpäset_siirto.csv", Utils.set(new Qname("MVL.224")));
		FILE_TO_INFORMAL_GROUP.put("Kääväkkäät_siirto.csv", Utils.set(new Qname("MVL.233")));
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
		FILE_TO_INFORMAL_GROUP.put("Perhoset_siirto2.csv", Utils.set(new Qname("MVL.31")));
		FILE_TO_INFORMAL_GROUP.put("Piensienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		FILE_TO_INFORMAL_GROUP.put("Pistiäiset_siirto.csv", Utils.set(new Qname("MVL.30")));
		FILE_TO_INFORMAL_GROUP.put("Pistiäiset_siirto_lisalataus.csv", Utils.set(new Qname("MVL.30")));
		FILE_TO_INFORMAL_GROUP.put("Punkit_siirto.csv", Utils.set(new Qname("MVL.234")));
		FILE_TO_INFORMAL_GROUP.put("Putkilokasvit_siirto.csv", Utils.set(new Qname("MVL.343")));
		FILE_TO_INFORMAL_GROUP.put("Ripsiäiset_siirto.csv", Utils.set(new Qname("MVL.228")));
		FILE_TO_INFORMAL_GROUP.put("Sammalet_siirto.csv", Utils.set(new Qname("MVL.23")));
		FILE_TO_INFORMAL_GROUP.put("Suorasiipiset_siirto.csv", Utils.set(new Qname("MVL.223"), new Qname("MVL.230"), new Qname("MVL.225")));
		FILE_TO_INFORMAL_GROUP.put("Sääsket_siirto.csv", Utils.set(new Qname("MVL.224")));
		FILE_TO_INFORMAL_GROUP.put("Tuhatjalkaiset_siirto.csv", Utils.set(new Qname("MVL.37")));
		FILE_TO_INFORMAL_GROUP.put("Verkkosiipiset_siirto.csv", Utils.set(new Qname("MVL.226")));
		FILE_TO_INFORMAL_GROUP.put("Vesiperhoset_siirto.csv", Utils.set(new Qname("MVL.222")));

		Set<Qname> all = new HashSet<>();
		for (Set<Qname> s : FILE_TO_INFORMAL_GROUP.values()) {
			all.addAll(s);
		}
		all.add(new Qname("MVL.40"));
		all.add(new Qname("MVL.30"));
		all.add(new Qname("MVL.213"));
		all.add(new Qname("MVL.462"));
		FILE_TO_INFORMAL_GROUP.put("yhdistetty.csv", all);
		FILE_TO_INFORMAL_GROUP.put("2010_korjattavat_28092018.csv", all);
	}

	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;

	public static void main(String[] args) {
		HikariDataSource dataSource = null;
		try {
			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"), new ErrorReportingToSystemErr());

			// prod mode XXX MUST USE PROD MODE WHEN LOADING DATA (dev is for test dry runs)
			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, false, triplestoreDAO, new ErrorReportingToSystemErr());

			// dev mode
			//taxonomyDAO = new ExtendedTaxonomyDAOImple(config, true, triplestoreDAO, new ErrorReportingToSystemErr());

			process();
			taxonomyDAO.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		}
		System.out.println("done");
	}

	private static void process() throws Exception {
		File folder = new File(FILE_PATH + EVALUATION_YEAR);
		for (File f : folder.listFiles()) {
			if (!f.isFile()) continue;
			if (!f.getName().endsWith(".csv")) continue;
			if (!f.getName().equals("Perhoset_siirto2.csv")) continue; // XXX load only one file here
			System.out.println(f.getName());
			process(f);
		}
		//writeDumps();
	}

	private static final Set<String> ONLY_THESE = Utils.set( // XXX load only certain lines (leave empty to disable this)
			"LE1503|MX.60231"
			);

	private static boolean shouldSkip(String line) {
		if (ONLY_THESE.isEmpty()) return false;
		for (String s : ONLY_THESE) {
			if (line.contains(s)) return false;
		}
		return true;
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
			if (shouldSkip(line)) continue;
			process(line, f, i, lines.size());
			//if (i > 5) break; // XXX
		}
	}

	private static void process(String line, File f, int i, int total) throws Exception {
		String[] parts = line.split(Pattern.quote("|"));
		//IUCNLineData data = new IUCNLineData(parts);
		IUCNLineData data = new IUCNLineData(Mode.V2010, parts); // XXX change parse mode here
		dump(data);
		Qname fixedQnameForName = getFixedQnameForName(data.scientificName);
		if (fixedQnameForName != null) data.taxonQname = fixedQnameForName.toString();
		process(data, f, i, total);
	}

	public static Qname getFixedQnameForName(String scientificName) throws Exception {
		Map<String, Qname> fixedNames = getFixedNames();
		return fixedNames.get(scientificName);
	}

	private static Map<String, Qname> fixedNames =  null;

	private static Map<String, Qname> getFixedNames() throws Exception {
		if (fixedNames == null) {
			fixedNames = new HashMap<>();
			File f= new File(FILE_PATH + "IUCN_aineistonsiirto_virheelliset_nimet.csv");
			for (String line : FileUtils.readLines(f)) {
				try {
					if (line.startsWith("Eliöryhmä")) continue;
					if (line.isEmpty()) continue;
					String[] parts = line.split(Pattern.quote("\t"));
					String name = parts[1];
					Qname qname = new Qname(parts[3].trim());
					fixedNames.put(name, qname);
				} catch (Exception e) {
					System.err.println(line);
					throw e;
				}
			}
		}
		return fixedNames;
	}

	private static void process(IUCNLineData data, File f, int i, int total) {
		try {
			System.out.println(i + " / " + total + "\t" + data.getScientificName() + " " + data.getTaxonQname());
			TaxonSearchResponse response = new TaxonSearchResponse(new TaxonSearch("foo"));
			if (given(data.getTaxonQname())) {
				response = taxonomyDAO.search(new TaxonSearch(data.getTaxonQname()).onlyExact());
			}
			if (response.getExactMatches().isEmpty() && given(data.getScientificName())) {
				response = taxonomyDAO.search(new TaxonSearch(data.getScientificName()).onlyExact());
				if (response.getExactMatches().size() > 1) {
					response = taxonomyDAO.search(new TaxonSearch(data.getScientificName()).onlyExact().addExlucedNameType(MISAPPLIED));
				}
			}
			if (response.getExactMatches().isEmpty() && given(data.getScientificName())) {
				String cleanedSciName = cleanScientificName(data.getScientificName());
				if (given(cleanedSciName)) {
					response = taxonomyDAO.search(new TaxonSearch(cleanedSciName).onlyExact());
				}
				if (response.getExactMatches().size() > 1) {
					if (given(cleanedSciName)) {
						response = taxonomyDAO.search(new TaxonSearch(cleanedSciName).onlyExact().addExlucedNameType(MISAPPLIED));
					}
				}
			}
			if (response.getExactMatches().isEmpty() && given(data.getFinnishName())) {
				response = taxonomyDAO.search(new TaxonSearch(data.getFinnishName()).onlyExact());
			}
			if (response.getExactMatches().isEmpty()) {
				for (String altname : data.getAlternativeFinnishNames()) {
					response = taxonomyDAO.search(new TaxonSearch(altname).onlyExact());
					if (!response.getExactMatches().isEmpty()) break;
				}
			}
			if (response.getExactMatches().isEmpty()) {
				reportTaxonNotFound("Tuntematon nimi", data, f);
				return;
			}
			Set<Qname> allowedInformalGroups = FILE_TO_INFORMAL_GROUP.get(f.getName());
			Set<Qname> matchingQnames = matchTaxonQnamesInAllowedInformalGroups(response.getExactMatches(), allowedInformalGroups);
			if (matchingQnames.size() == 1) {
				process(data, matchingQnames.iterator().next(), f);
				return;
			}
			if (matchingQnames.size() > 1) {
				reportTaxonNotFound("Löytyi " + matchingQnames.size() + " osumaa: " + matchingQnames, data, f);
				return;
			}
			StringBuilder message = new StringBuilder();
			message.append("Löytyi " + response.getExactMatches().size() + " osumaa, mutta väärästä lajiryhmästä: ");
			boolean hasGroups = false;
			for (Match match : response.getExactMatches()) {
				message.append(match.getTaxon().getQname().toString()).append(" ").append(debug(match.getInformalGroups())).append(" ");
				if (!match.getInformalGroups().isEmpty()) {
					hasGroups = true;
				}
			}
			if (!hasGroups) {
				if (response.getExactMatches().size() == 1 && !response.getExactMatches().get(0).getTaxon().hasParent()) {
					reportTaxonNotFound("Löytyi yksi osuma master checklististä, mutta parent puuttuu", data, f);
				} else {
					reportTaxonNotFound("Ei löydy yhdestäkään lajiryhmästä", data, f);
				}
			} else {
				reportTaxonNotFound(message.toString().trim(), data, f);
			}
		} catch (Exception e) {
			reportError(e, data, f);
		}
	}

	private static boolean given(String s) {
		return s != null && s.length() > 0;
	}

	public static String cleanScientificName(String scientificName) {
		if (scientificName.contains(",")) {
			return scientificName.split(Pattern.quote(","))[0].trim();
		}
		if (!scientificName.contains("(")) return null;
		StringBuilder b = new StringBuilder();
		boolean inparenthesis = false;
		for (char c : scientificName.toCharArray()) {
			if (c == '(') {
				inparenthesis = true;
				continue;
			}
			if (c == ')') {
				inparenthesis = false;
				continue;
			}
			if (!inparenthesis) {
				b.append(c);
			}
		}
		String s = b.toString();
		while (s.contains("  ")) {
			s = s.replace("  ", " ");
		}
		return s.trim();
	}

	private static String debug(List<InformalTaxonGroup> informalGroups) {
		StringBuilder b = new StringBuilder();
		for (InformalTaxonGroup i : informalGroups) {
			b.append(i.getName().forLocale("fi")).append(" ");
		}
		return b.toString().trim();
	}

	private static void process(IUCNLineData data, Qname taxonId, File f) throws Exception {
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		EvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(taxonId.toString());
		if (!target.getTaxon().isFinnish()) {
			if (target.getTaxon().getOccurrenceInFinland() == null) {
				reportTaxonNotFound("Ei ole merkitty suomalaiseksi", data, f);
				reportShouldBeMarkedFinnish(target);
				return;
			}
		}
		if (!target.getTaxon().isSpecies()) {
			reportTaxonNotFound("Ei ole laji vaan " + target.getTaxon().getTaxonRank(), data, f);
			return;
		}
		if (target.hasEvaluation(EVALUATION_YEAR)) {
			System.out.println("\t\t\t\t\t\t\t\t\tSkipping ... already has evaluation for " + EVALUATION_YEAR);
			return; // Already loaded
		}
		Evaluation evaluation = toEvaluation(taxonId, data, EVALUATION_YEAR);
		triplestoreDAO.store(evaluation, null); // XXX disable loading here
		//System.out.println(evaluation.getModel().getRDF() + " primary: " + evaluation.getPrimaryHabitat() + " secondary: " + evaluation.getSecondaryHabitats() + " threats: " + evaluation.getThreats() + " endangarment reasons: " + evaluation.getEndangermentReasons() + " occurrences " + evaluation.getOccurrences());
		iucnDAO.getIUCNContainer().setEvaluation(evaluation);
	}

	private static void reportShouldBeMarkedFinnish(EvaluationTarget target) {
		File file = new File("c:/temp/iucn/finnish_" + DateUtils.getCurrentDate() +".txt");
		try {
			Taxon t = target.getTaxon();
			String s = Utils.debugS(t.getQname(), t.getScientificName(), t.getScientificNameOfRank("genus"), t.getScientificNameOfRank("tribe"), t.getScientificNameOfRank("family"));
			FileUtils.writeToFile(file, s+"\n", "ISO-8859-1", true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static Evaluation toEvaluation(Qname taxonId, IUCNLineData data, int evaluationYear) throws Exception {
		Evaluation evaluation = taxonomyDAO.getIucnDAO().createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatementIfObjectGiven(Evaluation.IS_LOCKED, true);
		model.addStatementIfObjectGiven(Evaluation.STATE, new Qname(Evaluation.STATE_READY));
		model.addStatementIfObjectGiven(Evaluation.EVALUATED_TAXON, taxonId);
		model.addStatementIfObjectGiven(Evaluation.EVALUATION_YEAR, String.valueOf(evaluationYear));
		model.addStatementIfObjectGiven(Evaluation.BORDER_GAIN, data.getBorderGain());
		model.addStatementIfObjectGiven(Evaluation.BORDER_GAIN+NOTES, data.getBorderGainNotes());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_A, data.getCriteriaA());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_B, data.getCriteriaB());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_C, data.getCriteriaC());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_D, data.getCriteriaD());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_E, data.getCriteriaE());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_A+"Notes", data.criteriaANotes);
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_B+"Notes", data.criteriaBNotes);
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_C+"Notes", data.criteriaCNotes);
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_D+"Notes", data.criteriaDNotes);
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_E+"Notes", data.criteriaENotes);
		model.addStatementIfObjectGiven(Evaluation.STATUS_A, data.getCriteriaAStatus());
		model.addStatementIfObjectGiven(Evaluation.STATUS_B, data.getCriteriaBStatus());
		model.addStatementIfObjectGiven(Evaluation.STATUS_C, data.getCriteriaCStatus());
		model.addStatementIfObjectGiven(Evaluation.STATUS_D, data.getCriteriaDStatus());
		model.addStatementIfObjectGiven(Evaluation.STATUS_E, data.getCriteriaEStatus());
		model.addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, data.getCriteriaForStatus());
		model.addStatementIfObjectGiven(Evaluation.DECREASE_DURING_PERIOD+NOTES, data.getDecreaseDuringPeriodNotes());
		model.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MAX, s(data.getDistributionAreaMax()));
		model.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MIN, s(data.getDistributionAreaMin()));
		model.addStatementIfObjectGiven(Evaluation.DISTRIBUATION_AREA_NOTES, data.getDistributionAreaNotes());
		model.addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, s(data.getEvaluationPeriodLength()));
		model.addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH+NOTES, data.getEvaluationPeriodLengthNotes());
		model.addStatementIfObjectGiven(Evaluation.FRAGMENTED_HABITATS, data.getFragmentedHabitats());
		model.addStatementIfObjectGiven(Evaluation.FRAGMENTED_HABITATS+NOTES, data.getFragmentedHabitatsNotes());
		model.addStatementIfObjectGiven(Evaluation.GENERATION_AGE, s(data.getGenerationAge()));
		model.addStatementIfObjectGiven(Evaluation.GENERATION_AGE+NOTES, data.getGenerationAgeNotes());
		model.addStatementIfObjectGiven(Evaluation.GROUNDS_FOR_EVALUATION_NOTES, data.getGroundsForEvaluationNotes());
		model.addStatementIfObjectGiven(Evaluation.HABITAT_GENERAL_NOTES, data.getHabitatGeneralNotes());
		model.addStatementIfObjectGiven(Evaluation.HABITAT+NOTES, data.getHabitatNotes());
		model.addStatementIfObjectGiven(Evaluation.INDIVIDUAL_COUNT_MAX, s(data.getIndividualCountMax()));
		model.addStatementIfObjectGiven(Evaluation.INDIVIDUAL_COUNT_MIN, s(data.getIndividualCountMin()));
		model.addStatementIfObjectGiven(Evaluation.INDIVIDUAL_COUNT_NOTES, data.getIndividualCountNotes());
		model.addStatementIfObjectGiven(Evaluation.LAST_SIGHTING_NOTES, data.getLastSightingNotes());
		model.addStatementIfObjectGiven(Evaluation.LEGACY_PUBLICATIONS, data.getLegacyPublications());
		model.addStatementIfObjectGiven(Evaluation.LSA_RECOMMENDATION, data.getLsaRecommendation());
		model.addStatementIfObjectGiven(Evaluation.LSA_RECOMMENDATION+NOTES, data.getLsaRecommendationNotes());
		model.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MAX, s(data.getOccurrenceAreaMax()));
		model.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MIN, s(data.getOccurrenceAreaMin()));
		model.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_NOTES, data.getOccurrenceAreaNotes());
		model.addStatementIfObjectGiven(Evaluation.OCCURRENCE_NOTES, data.getOccurrenceNotes());
		model.addStatementIfObjectGiven(Evaluation.OCCURRENCE_REGIONS_NOTES, data.getOccurrenceRegionsNotes());
		model.addStatementIfObjectGiven(Evaluation.POPULATION_SIZE_PERIOD_BEGINNING, s(data.getPopulationSizePeriodBeginning()));
		model.addStatementIfObjectGiven(Evaluation.POPULATION_SIZE_PERIOD_END, s(data.getPopulationSizePeriodEnd()));
		model.addStatementIfObjectGiven(Evaluation.POPULATION_SIZE_PERIOD_NOTES, data.getPopulationSizePeriodNotes());
		model.addStatementIfObjectGiven(Evaluation.POPULATION_VARIES, data.getPopulationVaries());
		model.addStatementIfObjectGiven(Evaluation.POPULATION_VARIES+NOTES, data.getPopulationVariesNotes());
		model.addStatementIfObjectGiven(Evaluation.POSSIBLY_RE, data.getPossiblyRE());
		model.addStatementIfObjectGiven(Evaluation.POSSIBLY_RE+NOTES, data.getPossiblyRENotes());
		model.addStatementIfObjectGiven(Evaluation.REASON_FOR_STATUS_CHANGE+NOTES, data.getReasonForStatusChangeNotes());
		model.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, data.getRedListStatus());
		model.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MAX, data.getRedListStatusMax());
		model.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MIN, data.getRedListStatusMin());
		model.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_NOTES, data.getRedListStatusNotes());
		model.addStatementIfObjectGiven(Evaluation.TAXONOMIC_NOTES, data.getTaxonomicNotes());
		model.addStatementIfObjectGiven(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, data.getTypeOfOccurrenceInFinland());
		model.addStatementIfObjectGiven(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND+NOTES, data.getTypeOfOccurrenceInFinlandNotes());
		model.addStatementIfObjectGiven("MKV.redListStatusAccuracyNotes", data.redListStatusAccuracyNotes);
		model.addStatementIfObjectGiven(Evaluation.EXTERNAL_IMPACT, data.getExternalPopulationImpactOnRedListStatus());
		String editNotes = "Ladattu tiedostosta";
		if (data.editNotes != null) editNotes = data.editNotes;
		model.addStatementIfObjectGiven(Evaluation.EDIT_NOTES, editNotes);
		int i = 0;
		for (Qname q : data.getEndangermentReasons()) {
			evaluation.addEndangermentReason(new EndangermentObject(null, q, i++));
		}
		i = 0;
		for (Qname q : data.getThreats()) {
			evaluation.addThreat(new EndangermentObject(null, q, i++));
		}
		for (Map.Entry<Qname, Qname> e : data.getOccurrences().entrySet()) {
			Qname area = e.getKey();
			Qname status = e.getValue();
			Occurrence occurrence = new Occurrence(null, area, status);
			occurrence.setYear(EVALUATION_YEAR);
			evaluation.addOccurrence(occurrence);
		}
		evaluation.setPrimaryHabitat(data.getPrimaryHabitat());
		for (HabitatObject habitat : data.getSecondaryHabitats()) {
			evaluation.addSecondaryHabitat(habitat);
		}
		for (Qname q : data.getReasonForStatusChange()) {
			model.addStatementIfObjectGiven(Evaluation.REASON_FOR_STATUS_CHANGE, q);
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

	private static Set<Qname> matchTaxonQnamesInAllowedInformalGroups(List<Match> exactMatches, Set<Qname> allowedInformalGroups) {
		Set<Qname> set = new HashSet<>();
		for (Match m : exactMatches) {
			for (InformalTaxonGroup i : m.getInformalGroups()) {
				if (allowedInformalGroups.contains(i.getQname())) {
					set.add(m.getTaxon().getQname());
				}
			}
		}
		return set;
	}

	private static void reportTaxonNotFound(String message, IUCNLineData data, File f) {
		File file = new File("c:/temp/iucn/missing_" + f.getName().replace(".csv", "_" + DateUtils.getCurrentDate() +".txt"));
		try {
			StringBuilder b = new StringBuilder();
			String id = given(data.taxonQname) ? data.taxonQname : data.getScientificName();

			b.append(data.legacyInformalGroup).append("|");
			b.append(id).append("|");
			b.append(data.getFinnishName()).append("|");
			if (!data.getAlternativeFinnishNames().isEmpty()) b.append(data.getAlternativeFinnishNames().toString());
			b.append("|");
			b.append(data.redListStatus).append("|");
			b.append(message);
			FileUtils.writeToFile(file, b.toString()+"\n", "ISO-8859-1", true);
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
				if (value.length() > 30) value = "long text";
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
