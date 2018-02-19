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
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-least-concern/*"})
public class ApiMarkLeastConcernServler extends ApiMarkNotEvaluatedServler {

	private static final long serialVersionUID = 3567483532561265795L;

	@Override
	protected IUCNEvaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, HttpServletRequest req) throws Exception {
		IUCNEvaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.STATE), new ObjectResource(IUCNEvaluation.STATE_READY)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATED_TAXON), new ObjectResource(speciesQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.RED_LIST_STATUS), new ObjectResource("MX.iucnLC")));

		String notes = IUCNEvaluation.LC_MARK_NOTES + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EDIT_NOTES), new ObjectLiteral(notes)));

		String habitat = req.getParameter("habitat");
		String[] habitatSpecificTypes = req.getParameterValues("habitatSpecificType");
		
		if (!given(habitat)) {
			throw new IllegalStateException("Primary habitat must be given");
		}
		
		IUCNHabitatObject habitatObject = new IUCNHabitatObject(null, new Qname(habitat), 0);
		evaluation.setPrimaryHabitat(habitatObject);
		if (habitatSpecificTypes != null) {
			for (String type : habitatSpecificTypes) {
				if (given(type)) {
					habitatObject.addHabitatSpecificType(new Qname(type));
				}
			}
		}
		
		return evaluation;
	}

}
