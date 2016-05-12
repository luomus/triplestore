package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.xml.Document;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonomy-search-content/*"})
public class ApiTaxonSearchServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3382868354885463547L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = new ResponseData().setViewName("api-taxonomy-search");
		String searchword = getQname(req);
		String checklist = req.getParameter("checklist");
		if (!given(searchword)) {
			return responseData;
		}
		searchword = searchword.trim();
		
		Document response =  getTaxonomyDAO().search(searchword, checklist, 30);
		responseData.setData("response", response);
		responseData.setData("taxonpageBaseLinkURL", req.getParameter("taxonpageBaseLinkURL"));
		return responseData;
	}
	
}
