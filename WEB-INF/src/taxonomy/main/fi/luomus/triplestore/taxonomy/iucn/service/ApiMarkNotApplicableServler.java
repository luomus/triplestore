package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-applicable/*"})
public class ApiMarkNotApplicableServler extends ApiMarkNotEvaluatedServler {

	private static final long serialVersionUID = -555544719415746011L;

	@Override
	protected IUCNEvaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO) throws Exception {
		IUCNEvaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATED_TAXON), new ObjectResource(speciesQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.RED_LIST_STATUS), new ObjectResource("MX.iucnNA")));
		String notes = IUCNEvaluation.NA_MARK_NOTES + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EDIT_NOTES), new ObjectLiteral(notes)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.STATE), new ObjectResource(IUCNEvaluation.STATE_READY)));
		return evaluation;
	}

}
