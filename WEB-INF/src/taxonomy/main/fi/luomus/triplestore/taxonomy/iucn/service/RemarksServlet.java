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

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/remarks/*"})
public class RemarksServlet extends EvaluationEditServlet {

	private static final long serialVersionUID = -7749268274655196771L;
	private static final Predicate REMARKS_PREDICATE = new Predicate(IUCNEvaluation.REMARKS);

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		TriplestoreDAO dao = getTriplestoreDAO(req);
		String evaluationId = req.getParameter("evaluationId");
		String remarks = req.getParameter(REMARKS_PREDICATE.getQname());
		
		Model model = dao.get(evaluationId);
		if (model.isEmpty()) throw new IllegalStateException("No model for evaluation " + evaluationId);
		
		IUCNEvaluation evaluation = new IUCNEvaluation(model, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		String speciesQname = evaluation.getSpeciesQname();
		IUCNEvaluationTarget target = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getTarget(speciesQname);
		
		Subject subject = new Subject(evaluationId);
		Statement statement = new Statement(REMARKS_PREDICATE, new ObjectLiteral(remarks)); 
		dao.store(subject, statement);
		
		model.removeAll(REMARKS_PREDICATE);
		model.addStatement(statement);
		target.setEvaluation(evaluation);
		
		getSession(req).setFlashSuccess("Kommentit tallennettu!");
		
		return redirectTo(getConfig().baseURL()+"/iucn/species/"+speciesQname+"/"+evaluation.getEvaluationYear(), res);
	}

}
