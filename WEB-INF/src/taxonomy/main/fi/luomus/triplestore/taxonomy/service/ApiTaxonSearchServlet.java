package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
import fi.luomus.commons.xml.Document;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonomy-search-content/*"})
public class ApiTaxonSearchServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3382868354885463547L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = new ResponseData().setViewName("api-taxonomy-search");
		String searchword = req.getParameter("q");
		if (!given(searchword)) {
			return responseData;
		}
		Qname checklist = PublicTaxonSearchApiServlet.parseChecklist(req);
		boolean onlySpecies = "true".equals(req.getParameter("onlySpecies"));
		boolean onlyFinnish = "true".equals(req.getParameter("onlyFinnish"));
		searchword = searchword.trim();
		Document response =  getTaxonomyDAO().search(new TaxonSearch(searchword, 30, checklist).setOnlySpecies(onlySpecies).setOnlyFinnish(onlyFinnish));
		responseData.setData("response", response);
		responseData.setData("taxonpageBaseLinkURL", req.getParameter("taxonpageBaseLinkURL"));
		return responseData;
	}
	
}
