package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/remarks/*"})
public class RemarksServlet extends EvaluationEditServlet {

	private static final long serialVersionUID = -7749268274655196771L;
	private static final Predicate REMARKS_PREDICATE = new Predicate(Evaluation.REMARKS);

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		TriplestoreDAO dao = getTriplestoreDAO(req);
		String evaluationId = req.getParameter("evaluationId");
		String remarks = req.getParameter(REMARKS_PREDICATE.getQname());
		String deleteStatementId = req.getParameter("delete");
		
		Model model = dao.get(evaluationId);
		if (model.isEmpty()) throw new IllegalStateException("No model for evaluation " + evaluationId);

		Evaluation evaluation = new Evaluation(model, dao.getProperties(Evaluation.EVALUATION_CLASS));
		String speciesQname = evaluation.getSpeciesQname();
		Container container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
		EvaluationTarget target = container.getTarget(speciesQname);

		if (given(remarks)) {
			String userFullname = getUser(req).getFullname();
			String date = DateUtils.getCurrentDateTime("dd.MM.yyyy");
			remarks = userFullname + " " + date + ":\n" + remarks;
			
			Subject subject = new Subject(evaluationId);
			Statement statement = new Statement(REMARKS_PREDICATE, new ObjectLiteral(remarks)); 
			dao.insert(subject, statement);

			model = dao.get(evaluationId); // must get model again for added statement to have a statement id
			evaluation = new Evaluation(model, dao.getProperties(Evaluation.EVALUATION_CLASS));
			evaluation.setIncompletelyLoaded(true);
			container.setEvaluation(evaluation);
			container.addRemark(target, evaluation);
			getSession(req).setFlashSuccess("Kommentit tallennettu!");
		} else if (given(deleteStatementId)) {
			if (!permissions(req, target, evaluation)) throw new IllegalAccessException();
			int id = Integer.valueOf(deleteStatementId);
			boolean found = model.removeStatement(id);
			if (found) {
				// important not to delete statements that are not found from the model.. they could be any statements
				dao.deleteStatement(id);
				evaluation.setIncompletelyLoaded(true);
				container.setEvaluation(evaluation);
				container.removeRemark(target, id);
				getSession(req).setFlashSuccess("Kommentti poistettu!");
			} else {
				getSession(req).setFlashSuccess("Ei mitään poistettavaa!");
			}
		} else {
			getSession(req).setFlashSuccess("Ei mitään tallennettavaa!");
		}
		
		return redirectTo(getConfig().baseURL()+"/iucn/species/"+speciesQname+"/"+evaluation.getEvaluationYear());
	}

}
