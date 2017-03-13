package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/singleTaxonInfo/*"})
public class ApiSingleTaxonInfoServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 1893676443827002355L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-singleTaxonInfo");
		
		String taxonQname = getId(req);
		if (!taxonQname.contains(".")) taxonQname = taxonQname.replace("MX", "MX.");
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		
		return responseData.setData("taxon", taxon);
	}
	
}
