package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Evaluation;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-applicable/*"})
public class ApiMarkNotApplicableServler extends ApiMarkNotEvaluatedServler {

	private static final long serialVersionUID = -555544719415746011L;

	@Override
	protected Evaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, HttpServletRequest req) throws Exception {
		Evaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(new Predicate(Evaluation.STATE), new ObjectResource(Evaluation.STATE_READY)));
		model.addStatement(new Statement(new Predicate(Evaluation.EVALUATED_TAXON), new ObjectResource(speciesQname)));
		model.addStatement(new Statement(new Predicate(Evaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(new Predicate(Evaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(Evaluation.LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		model.addStatement(new Statement(new Predicate(Evaluation.RED_LIST_STATUS), new ObjectResource(Evaluation.NA)));
		
		String notes = Evaluation.NA_MARK_NOTES + Evaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(new Predicate(Evaluation.EDIT_NOTES), new ObjectLiteral(notes)));
		
		String typeOfOccurrenceInFinland = req.getParameter("typeOfOccurrenceInFinland");
		model.addStatementIfObjectGiven(new Predicate(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND), new Qname(typeOfOccurrenceInFinland));
		return evaluation;
	}

}
