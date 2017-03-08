package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/synonymsOfTaxon/*"})
public class ApiSynonymsOfTaxonServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 1893676443827002355L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-synonymsOfTaxon");
		
		String synonymParentQname = getId(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon synonymParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(synonymParentQname));
		
		return responseData.setData("synonyms", synonymParent.getSynonyms());
	}
	
}
