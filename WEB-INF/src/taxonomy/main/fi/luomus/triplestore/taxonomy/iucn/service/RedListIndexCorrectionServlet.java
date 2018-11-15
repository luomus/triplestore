package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/redListIndexCorrection/*"})
public class RedListIndexCorrectionServlet extends EvaluationEditServlet {

	private static final Predicate LAST_MODIFIED_BY_PREDICATE = new Predicate(IUCNEvaluation.LAST_MODIFIED_BY);
	private static final Predicate LAST_MODIFIED_PREDICATE = new Predicate(IUCNEvaluation.LAST_MODIFIED);
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
		IUCNContainer container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
		IUCNEvaluationTarget target = container.getTarget(speciesQname);

		if (!permissions(req, target, null)) {
			throw new IllegalAccessException();
		}

		boolean indexAdded = given(redListIndexCorrection);

		String editNotes = editNotes(indexAdded);
		Qname editor = getUser(req).getQname();

		
		Subject subject = new Subject(evaluationId);
		Statement indexStatement = indexAdded ? new Statement(INDEX_PREDICATE, new ObjectResource(redListIndexCorrection)) : null;
		Statement notesStatement = new Statement(NOTES_PREDICATE, new ObjectLiteral(redListIndexCorrectionNotes)); 
		Statement editNotesStatement = new Statement(IucnDAO.EDIT_NOTES_PREDICATE, new ObjectLiteral(editNotes));
		Statement lastModifiedStatement = new Statement(LAST_MODIFIED_PREDICATE, new ObjectLiteral(DateUtils.getCurrentDate()));
		Statement lastModifiedByStatement = new Statement(LAST_MODIFIED_BY_PREDICATE, new ObjectResource(editor));
		
		if (indexAdded) {
			dao.store(subject, indexStatement);
		} else {
			dao.delete(subject, INDEX_PREDICATE);
		}
		dao.store(subject, notesStatement);
		dao.store(subject, editNotesStatement);
		dao.store(subject, lastModifiedStatement);
		dao.store(subject, lastModifiedByStatement);

		model.removeAll(INDEX_PREDICATE);
		model.removeAll(NOTES_PREDICATE);
		model.removeAll(IucnDAO.EDIT_NOTES_PREDICATE);
		model.removeAll(LAST_MODIFIED_PREDICATE);
		model.removeAll(LAST_MODIFIED_BY_PREDICATE);
		
		if (indexAdded) {
			model.addStatement(indexStatement);
		}
		model.addStatement(notesStatement);
		model.addStatement(editNotesStatement);
		model.addStatement(lastModifiedStatement);
		model.addStatement(lastModifiedByStatement);

		evaluation.setIncompletelyLoaded(true);
		container.setEvaluation(evaluation);

		getSession(req).setFlashSuccess("Indeksi tallennettu!");

		return redirectTo(getConfig().baseURL()+"/iucn/species/"+speciesQname+"/"+evaluation.getEvaluationYear());
	}

	private String editNotes(boolean indexAdded) {
		return indexAdded ? 
				IUCNEvaluation.INDEX_CHANGE_NOTES + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy") : 
					IUCNEvaluation.INDEX_REMOVE_NOTES + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy");
	}

}
