package fi.luomus.triplestore.taxonomy.iucn.runnable;

import java.io.File;
import java.io.IOException;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidator;

public class IUCNValidointi {

	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;
	private static DataSource dataSource;
	private static ErrorReporter errorReporter = new ErrorReporingToSystemErr();
	private static File errorFile = new File("c:/temp/iucn/validation_" + DateUtils.getFilenameDatetime() + ".txt");
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

		// prod mode XXX MUST USE PROD MODE WHEN LOADING DATA (dev is for test dry runs)
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, false, triplestoreDAO, new ErrorReporingToSystemErr()); 
		//taxonomyDAO = new ExtendedTaxonomyDAOImple(config, true, triplestoreDAO, new ErrorReporingToSystemErr());

		taxonomyDAO.getIucnDAO().getIUCNContainer().getTarget("MX.1");
		validate();
		taxonomyDAO.close();
	}

	private static void validate() throws Exception {
		for (IUCNEvaluationTarget target : taxonomyDAO.getIucnDAO().getIUCNContainer().getTargets()) {
			if (!target.hasEvaluation(2019)) continue;
			IUCNEvaluation evaluation = target.getEvaluation(2019);
			if (!evaluation.isReady()) continue;
			if (evaluation.isIncompletelyLoaded()) {
				taxonomyDAO.getIucnDAO().completeLoading(evaluation);
			}
			IUCNEvaluation comparisonData = target.getPreviousEvaluation(2019);
			validate(evaluation, comparisonData);
		}
	}

	private static void validate(IUCNEvaluation evaluation, IUCNEvaluation comparisonData) throws IOException {
		IUCNValidationResult result = new IUCNValidator(triplestoreDAO, errorReporter).validate(evaluation, comparisonData);
		System.out.println(evaluation.getSpeciesQname() + " -> " + ok(result.hasErrors()));
		if (result.hasErrors()) {
			reportErrors(evaluation, result);
		}
	}

	private static String ok(boolean hasErrors) {
		if (hasErrors) return "errors";
		return "ok";
	}

	private static void reportErrors(IUCNEvaluation evaluation, IUCNValidationResult result) throws IOException {
		StringBuilder b = new StringBuilder();
		b.append(evaluation.getSpeciesQname()).append("|").append(evaluation.getEvaluationYear()).append("|");
		for (String error : result.listErrors()) {
			b.append(error).append("|");
		}
		b.append("\n");
		FileUtils.writeToFile(errorFile, b.toString(), true);
	}

}
