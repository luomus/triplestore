package fi.luomus.triplestore.taxonomy.iucn.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/redListIndexCorrection/*"})
public class RedListIndexCorrectionServlet extends EvaluationEditServlet {

	private static final Predicate NOTES_PREDICATE = new Predicate(IUCNEvaluation.RED_LIST_INDEX_CORRECTION+"Notes");
	private static final Predicate INDEX_PREDICATE = new Predicate(IUCNEvaluation.RED_LIST_INDEX_CORRECTION);
	private static final long serialVersionUID = 2285910485664606619L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		TriplestoreDAO dao = getTriplestoreDAO(req);
		String evaluationId = req.getParameter("evaluationId");
		String redListIndexCorrection = req.getParameter(INDEX_PREDICATE.getQname());
		String redListIndexCorrectionNotes = req.getParameter(NOTES_PREDICATE.getQname());
		
		Model model = dao.get(evaluationId);
		if (model.isEmpty()) throw new IllegalStateException("No model for evaluation " + evaluationId);
		
		IUCNEvaluation evaluation = new IUCNEvaluation(model, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		String speciesQname = evaluation.getSpeciesQname();
		IUCNEvaluationTarget target = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getTarget(speciesQname);
		
		if (!permissions(req, target, null)) {
			throw new IllegalAccessException();
		}
				
		Subject subject = new Subject(evaluationId);
		Statement indexStatement = new Statement(INDEX_PREDICATE, new ObjectLiteral(redListIndexCorrection));
		Statement notesStatement = new Statement(NOTES_PREDICATE, new ObjectLiteral(redListIndexCorrectionNotes)); 
		dao.store(subject, indexStatement);
		dao.store(subject, notesStatement);
		
		model.removeAll(INDEX_PREDICATE);
		model.removeAll(NOTES_PREDICATE);
		model.addStatement(indexStatement);
		model.addStatement(notesStatement);
		target.setEvaluation(evaluation);
		
		getSession(req).setFlashSuccess("Indeksi tallennettu!");
		
		return redirectTo(getConfig().baseURL()+"/iucn/species/"+speciesQname+"/"+evaluation.getEvaluationYear(), res);
	}

}
