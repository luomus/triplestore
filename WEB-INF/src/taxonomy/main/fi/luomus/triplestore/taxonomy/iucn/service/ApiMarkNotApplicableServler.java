package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-applicable/*"})
public class ApiMarkNotApplicableServler extends ApiMarkNotEvaluatedServler {

	private static final long serialVersionUID = -555544719415746011L;

	@Override
	protected Evaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, HttpServletRequest req) throws Exception {
		Evaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(Predicate.of(Evaluation.STATE), ObjectResource.of(Evaluation.STATE_READY)));
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATED_TAXON), ObjectResource.of(speciesQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED_BY), ObjectResource.of(editorQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.RED_LIST_STATUS), ObjectResource.of(Evaluation.NA)));
		
		String notes = Evaluation.NA_MARK_NOTES + Evaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(Predicate.of(Evaluation.EDIT_NOTES), new ObjectLiteral(notes)));
		
		String typeOfOccurrenceInFinland = req.getParameter("typeOfOccurrenceInFinland");
		model.addStatementIfObjectGiven(Predicate.of(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND), Qname.of(typeOfOccurrenceInFinland));
		return evaluation;
	}

}
