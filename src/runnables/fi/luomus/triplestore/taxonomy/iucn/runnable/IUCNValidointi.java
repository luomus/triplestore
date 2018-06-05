package fi.luomus.triplestore.taxonomy.iucn.runnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

	private static final int VALIDATION_YEAR = 2019;
	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;
	private static DataSource dataSource;
	private static ErrorReporter errorReporter = new ErrorReporingToSystemErr();
	private static File validationFile = new File("c:/temp/iucn/validation_" + DateUtils.getFilenameDatetime() + ".txt");
	private static File statusChangeFile = new File("c:/temp/iucn/status_change_" + DateUtils.getFilenameDatetime() + ".txt");

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

		// all data mode (prod) XXX
		//taxonomyDAO = new ExtendedTaxonomyDAOImple(config, false, triplestoreDAO, new ErrorReporingToSystemErr());
		// limited data mode (test)
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, true, triplestoreDAO, new ErrorReporingToSystemErr());

		taxonomyDAO.getIucnDAO().getIUCNContainer().getTarget("MX.1");
		validate();
		taxonomyDAO.close();
	}

	private static void validate() throws Exception {
		initFileHeaders();
		for (IUCNEvaluationTarget target : taxonomyDAO.getIucnDAO().getIUCNContainer().getTargets()) {
			if (!target.hasEvaluation(VALIDATION_YEAR)) continue;
			IUCNEvaluation evaluation = target.getEvaluation(VALIDATION_YEAR);
			if (!evaluation.isReady()) continue;
			if (evaluation.isIncompletelyLoaded()) {
				taxonomyDAO.getIucnDAO().completeLoading(evaluation);
			}
			IUCNEvaluation previusEvaluation = target.getPreviousEvaluation(VALIDATION_YEAR);
			validate(evaluation, previusEvaluation);
			if (statusChanges(evaluation, previusEvaluation)) {
				reportStatusChange(evaluation, target);
			}
		}
	}

	private static void initFileHeaders() throws Exception {
		List<String> commonHeaders = Utils.list("ID", "Taxon ID", "Tieteellinen nimi", "Lajiryhmät");

		List<String> validationHeaders = new ArrayList<>(commonHeaders);
		validationHeaders.add("Virheet");

		List<String> statusChangeHeaders = new ArrayList<>(commonHeaders);
		statusChangeHeaders.add("Muutoksen syy");
		GroupSpeciesListServlet.appendRLIHeader(VALIDATION_YEAR, taxonomyDAO.getIucnDAO().getEvaluationYears(), statusChangeHeaders);

		report(validationFile, validationHeaders);
		report(statusChangeFile, statusChangeHeaders);
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

	private static void report(File file, IUCNEvaluation evaluation, List<String> values) {
		Taxon taxon = taxonomyDAO.getTaxon(new Qname(evaluation.getSpeciesQname()));
		List<String> theseValues = Utils.list(
				evaluation.getId(),
				evaluation.getSpeciesQname(),
				taxon.getScientificName(),
				informalGroups(taxon.getInformalTaxonGroups())
				);
		theseValues.addAll(values);
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
		List<InformalTaxonGroup> informalTaxonGroups = new ArrayList<>();
		for (Qname qname : informalTaxonGroupIds) {
			informalTaxonGroups.add(taxonomyDAO.getInformalTaxonGroups().get(qname.toString()));
		}
		Collections.sort(informalTaxonGroups);
		return informalGroups(informalTaxonGroups);
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
