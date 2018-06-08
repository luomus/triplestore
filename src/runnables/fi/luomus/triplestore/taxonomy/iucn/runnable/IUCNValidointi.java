package fi.luomus.triplestore.taxonomy.iucn.runnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidator;
import fi.luomus.triplestore.taxonomy.iucn.service.GroupSpeciesListServlet;

public class IUCNValidointi {

	private static final String WAKE_UP_TAXON = "MX.1";
	private static final int VALIDATION_YEAR = 2019;
	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;
	private static DataSource dataSource;
	private static ErrorReporter errorReporter = new ErrorReporingToSystemErr();
	private static File validationFile = new File("c:/temp/iucn/validointi_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File statusChangeFile = new File("c:/temp/iucn/luokka_muuttunut_RLI_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File automaticChangesFile = new File("c:/temp/iucn/automaattiset_muutokset_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File missing2010File = new File("c:/temp/iucn/2010_puuttuu_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File missing2019File = new File("c:/temp/iucn/2019_puuttuu_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File notReadyFile = new File("c:/temp/iucn/ei_merkitty_valmiiksi_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File groupErrorsFile = new File("c:/temp/iucn/ryhmissä_vikaa_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File targetNoEvalsFile = new File("c:/temp/iucn/!target_no_evals_" + DateUtils.getFilenameDatetime() + ".txt");

	public static void main(String[] args) {
		try {
			initAndValidate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		}
		System.out.println("done");
	}

	private static void initAndValidate() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));

		// all data mode XXX
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, false, triplestoreDAO, new ErrorReporingToSystemErr());
		// limited data mode
		// taxonomyDAO = new ExtendedTaxonomyDAOImple(config, true, triplestoreDAO, new ErrorReporingToSystemErr());

		taxonomyDAO.getIucnDAO().getIUCNContainer().getTarget(WAKE_UP_TAXON);
		validate();
		taxonomyDAO.close();
	}

	private static void validate() throws Exception {
		initFileHeaders();
		Collection<IUCNEvaluationTarget> targets = taxonomyDAO.getIucnDAO().getIUCNContainer().getTargets();
		int i = 1;
		for (IUCNEvaluationTarget target : targets) {
			System.out.println((i++) + "/" + targets.size() + " " + target.getQname());
			Set<Qname> targetEvaluatedGroups = getEvaluatedGroups(target);
			if (!target.hasEvaluations()) {
				if (!target.getQname().equals(WAKE_UP_TAXON)) {
					report(targetNoEvalsFile, Utils.list(target.getTaxon().getQname().toString()));
				}
				continue;
			}
			if (targetEvaluatedGroups.size() > 1) {
				report(groupErrorsFile, target.getLatestEvaluation(), Utils.list("Useita ryhmiä"));
			}
			if (targetEvaluatedGroups.isEmpty()) {
				report(groupErrorsFile, target.getLatestEvaluation(), Utils.list("Ei missään ryhmässä"));
			}

			if (!target.hasEvaluation(VALIDATION_YEAR)) {
				if (target.hasEvaluations()) {
					report(missing2019File, target.getLatestEvaluation());
				}
				continue;
			}
			IUCNEvaluation evaluation = target.getEvaluation(VALIDATION_YEAR);
			if (!evaluation.isReady()) {
				report(notReadyFile, evaluation);
				continue;
			}
			if (evaluation.isIncompletelyLoaded()) {
				taxonomyDAO.getIucnDAO().completeLoading(evaluation);
			}
			IUCNEvaluation previusEvaluation = target.getPreviousEvaluation(VALIDATION_YEAR);
			validate(evaluation, previusEvaluation);
			if (statusChanges(evaluation, previusEvaluation)) {
				reportStatusChange(evaluation, target);
			}
			listAutomaticChanges(evaluation, previusEvaluation);
			if (!target.hasEvaluation(2010) && (target.hasEvaluation(2000) || target.hasEvaluation(2015))) {
				report(missing2010File, evaluation);
			}
		}
	}

	private static Set<Qname> getEvaluatedGroups(IUCNEvaluationTarget target) throws Exception {
		Set<String> evaluationGroups = taxonomyDAO.getIucnDAO().getGroupEditors().keySet();
		Set<Qname> targetEvaluatedGroups = new HashSet<>();
		for (Qname taxonGroup : target.getTaxon().getInformalTaxonGroups()) {
			if (evaluationGroups.contains(taxonGroup.toString())) {
				targetEvaluatedGroups.add(taxonGroup);
			}
		}
		return targetEvaluatedGroups;
	}

	private static void initFileHeaders() throws Exception {
		List<String> commonHeaders = Utils.list("Arvioinnin ID", "Vuosi", "Luokka", "Taksonin ID", "Tieteellinen nimi", "Lajiryhmät");

		List<String> validationHeaders = new ArrayList<>(commonHeaders);
		validationHeaders.add("Virheet");

		List<String> statusChangeHeaders = new ArrayList<>(commonHeaders);
		statusChangeHeaders.add("Muutoksen syy");
		GroupSpeciesListServlet.appendRLIHeader(VALIDATION_YEAR, taxonomyDAO.getIucnDAO().getEvaluationYears(), statusChangeHeaders);

		List<String> automatedChangesHeaders = new ArrayList<>(commonHeaders);
		automatedChangesHeaders.add("Muutos");

		List<String> groupErrorsHeaders = new ArrayList<>(commonHeaders);
		groupErrorsHeaders.add("Virhe");

		report(validationFile, validationHeaders);
		report(statusChangeFile, statusChangeHeaders);
		report(automaticChangesFile, automatedChangesHeaders);
		report(missing2010File, commonHeaders);
		report(missing2019File, commonHeaders);
		report(notReadyFile, commonHeaders);
		report(groupErrorsFile, groupErrorsHeaders);
	}

	private static final Set<String> EN_CR = Utils.set("MX.iucnEN", "MX.iucnCR");
	private static final Set<String> RE_DD_NA_NE = Utils.set("MX.iucnRE", "MX.iucnDD", "MX.iucnNA", "MX.iucnNE");
	private static final Set<String> LC_RE_DD_NA_NE = Utils.set("MX.iucnLC", "MX.iucnRE", "MX.iucnDD", "MX.iucnNA", "MX.iucnNE");

	private static void listAutomaticChanges(IUCNEvaluation evaluation, IUCNEvaluation previusEvaluation) {
		if ("MKV.reasonForStatusChangeChangesInCriteria".equals(evaluation.getValue("MKV.reasonForStatusChange"))) {
			reportAutomaticChange(evaluation, "Muutoksen syy 3 - > 8");
		}
		if (previusEvaluation != null && previusEvaluation.getEvaluationYear().intValue() == 2010) {
			if (evaluation.hasValue("MKV.reasonForStatusChange") && !statusChanges(evaluation, previusEvaluation)) {
				reportAutomaticChange(evaluation, "Muutoksen syy poistetaan, koska luokat ovat " + s(evaluation.getIucnStatus()) + " <-> " + s(previusEvaluation.getIucnStatus()));
			}
		}
		if (EN_CR.contains(evaluation.getIucnStatus())) {
			if ("D1".equals(evaluation.getValue("MKV.criteriaForStatus"))) {
				reportAutomaticChange(evaluation, "Kriteeri D1 -> D, koska luokka on " + s(evaluation.getIucnStatus()));
			}
		}
		if (RE_DD_NA_NE.contains(evaluation.getIucnStatus())) {
			if (evaluation.hasValue("MKV.exteralPopulationImpactOnRedListStatus")) {
				reportAutomaticChange(evaluation, "Poistetaan luokan alenn./korott., koska luokka on " + s(evaluation.getIucnStatus()));
			}
		}
		if (LC_RE_DD_NA_NE.contains(evaluation.getIucnStatus())) {
			if (evaluation.hasValue("MKV.criteriaForStatus")) {
				reportAutomaticChange(evaluation, "Poistetaan kriteerit, koska luokka on " + s(evaluation.getIucnStatus()));
			}
		}
		if (evaluation.hasValue("MKV.ddReason") && !"MX.iucnDD".equals(evaluation.getIucnStatus())) {
			reportAutomaticChange(evaluation, "Poistetaan DD-syy, koska luokka on " + s(evaluation.getIucnStatus()));
		}
	}

	private static String s(String iucnStatus) {
		if (iucnStatus == null) return "";
		return iucnStatus.replace("MX.iucn", "");
	}

	private static void reportAutomaticChange(IUCNEvaluation evaluation, String change) {
		report(automaticChangesFile, evaluation, Utils.list(change));
	}

	private static boolean statusChanges(IUCNEvaluation evaluation, IUCNEvaluation previusEvaluation) {
		if (previusEvaluation == null) return false;
		String status = evaluation.getIucnStatus();
		String prevStatus = previusEvaluation.getIucnStatus();
		if (status == null || prevStatus == null) return false;
		if ("MX.iucnNE".equals(status) || "MX.iucnNE".equals(prevStatus)) return false;
		return !status.equals(prevStatus);
	}

	private static void reportStatusChange(IUCNEvaluation evaluation, IUCNEvaluationTarget target) throws Exception {
		List<String> values = new ArrayList<>();
		values.add(evaluation.getValue("MKV.reasonForStatusChange"));
		List<Integer> years = taxonomyDAO.getIucnDAO().getEvaluationYears();
		GroupSpeciesListServlet.appendRLIValues(target, years, VALIDATION_YEAR, values);
		report(statusChangeFile, evaluation, values);
	}

	private static void validate(IUCNEvaluation evaluation, IUCNEvaluation comparisonData) {
		IUCNValidationResult result = new IUCNValidator(triplestoreDAO, errorReporter).validate(evaluation, comparisonData);
		if (result.hasErrors()) {
			reportValidationError(evaluation, result);
		}
	}

	private static void reportValidationError(IUCNEvaluation evaluation, IUCNValidationResult result) {
		report(validationFile, evaluation, result.listErrors());
	}


	private static void report(File file, IUCNEvaluation evaluation) {
		report(file, evaluation, null);
	}

	private static void report(File file, IUCNEvaluation evaluation, List<String> values) {
		Taxon taxon = taxonomyDAO.getTaxon(new Qname(evaluation.getSpeciesQname()));
		List<String> theseValues = Utils.list(
				evaluation.getId(),
				evaluation.getEvaluationYear().toString(),
				s(evaluation.getIucnStatus()),
				evaluation.getSpeciesQname(),
				taxon.getScientificName(),
				informalGroups(taxon.getInformalTaxonGroups())
				);
		if (values != null) {
			theseValues.addAll(values);
		}
		report(file, theseValues);
	}

	private static void report(File file, List<String> values) {
		StringBuilder b = new StringBuilder();
		for (String value : values) {
			b.append(value).append("|");
		}
		b.append("\n");
		try {
			FileUtils.writeToFile(file, b.toString(), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String informalGroups(Set<Qname> informalTaxonGroupIds) {
		try {
			Set<String> evaluatedGroups = taxonomyDAO.getIucnDAO().getGroupEditors().keySet();
			List<InformalTaxonGroup> informalTaxonGroups = new ArrayList<>();
			for (Qname qname : informalTaxonGroupIds) {
				if (evaluatedGroups.contains(qname.toString())) {
					informalTaxonGroups.add(taxonomyDAO.getInformalTaxonGroups().get(qname.toString()));
				}
			}
			Collections.sort(informalTaxonGroups);
			return informalGroups(informalTaxonGroups);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String informalGroups(List<InformalTaxonGroup> informalGroups) {
		StringBuilder b = new StringBuilder();
		Iterator<InformalTaxonGroup> i = informalGroups.iterator();
		while (i.hasNext()) {
			InformalTaxonGroup group = i.next();
			b.append(group.getName().forLocale("fi"));
			if (i.hasNext()) {
				b.append("; ");
			}
		}
		return b.toString().trim();
	}

}
