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
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-least-concern/*"})
public class ApiMarkLeastConcernServler extends ApiMarkNotEvaluatedServler {

	private static final long serialVersionUID = 3567483532561265795L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String speciesQname = req.getParameter("speciesQname");
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(Qname.of(speciesQname));
		getTaxonomyDAO().addHabitats(taxon);
		TriplestoreDAO dao = getTriplestoreDAO();
		return new ResponseData().setViewName("iucn-lc-form")
				.setData("taxon", taxon)
				.setData("evaluationProperties", getTaxonomyDAO().getIucnDAO().getEvaluationProperties())
				.setData("habitatObjectProperties", dao.getProperties(Evaluation.HABITAT_OBJECT_CLASS))
				.setData("habitatLabelIndentator", FrontpageServlet.getHabitatLabelIndentaror(dao));
	}
	
	@Override
	protected Evaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, HttpServletRequest req) throws Exception {
		Evaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(Predicate.of(Evaluation.STATE), ObjectResource.of(Evaluation.STATE_READY)));
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATED_TAXON), ObjectResource.of(speciesQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED_BY), ObjectResource.of(editorQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.RED_LIST_STATUS), ObjectResource.of(Evaluation.LC)));

		String notes = Evaluation.LC_MARK_NOTES + Evaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(Predicate.of(Evaluation.EDIT_NOTES), new ObjectLiteral(notes)));

		String habitat = req.getParameter("habitat");
		String[] habitatSpecificTypes = req.getParameterValues("habitatSpecificType");
		
		if (!given(habitat)) {
			throw new IllegalStateException("Primary habitat must be given");
		}
		
		HabitatObject habitatObject = new HabitatObject(null, Qname.of(habitat), 0);
		evaluation.setPrimaryHabitat(habitatObject);
		if (habitatSpecificTypes != null) {
			for (String type : habitatSpecificTypes) {
				if (given(type)) {
					habitatObject.addHabitatSpecificType(Qname.of(type));
				}
			}
		}
		
		return evaluation;
	}

}
