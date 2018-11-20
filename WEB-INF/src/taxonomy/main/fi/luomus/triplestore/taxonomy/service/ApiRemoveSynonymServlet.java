package fi.luomus.triplestore.taxonomy.service;

import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.service.ApiAddSynonymServlet.SynonymType;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/removeSynonym/*"})
public class ApiRemoveSynonymServlet extends ApiBaseServlet {

	private static final String REMOVED_ID_PARAMETER = "removedId";
	private static final long serialVersionUID = 7393608674235660598L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		SynonymType synonymType = ApiAddSynonymServlet.getSynonymType(req);
		String removedId = getRemovedId(req);
		EditableTaxon synonymParent = ApiAddSynonymServlet.getSynonymParent(req, dao, taxonomyDAO);
		
		checkPermissionsToAlterTaxon(synonymParent, req);
		
		Model synonymParentModel = dao.get(synonymParent.getQname());
		for (Statement s : getSynonymStatements(synonymType, synonymParentModel)) {
			if (s.isResourceStatement() && s.getObjectResource().getQname().equals(removedId)) {
				dao.deleteStatement(s.getId());
			}
		}
		
		synonymParent.invalidateSelf();
		
		return apiSuccessResponse(res);
	}

	public static List<Statement> getSynonymStatements(SynonymType synonymType, Model synonymParentModel) {
		return synonymParentModel.getStatements(ApiAddSynonymServlet.getPredicate(synonymType).getQname());
	}

	private String getRemovedId(HttpServletRequest req) {
		String id = req.getParameter(REMOVED_ID_PARAMETER);
		if (id == null) throw new IllegalArgumentException("Must give " + REMOVED_ID_PARAMETER + " parameter");
 		id = id.replace("MX", "MX.");
 		return id;
	}
	
}
