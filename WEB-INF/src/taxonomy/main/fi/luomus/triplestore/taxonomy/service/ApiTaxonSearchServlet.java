package fi.luomus.triplestore.taxonomy.service;

import java.util.Collections;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.triplestore.models.User;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonomy-search-content/*"})
public class ApiTaxonSearchServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3382868354885463547L;

	private static final Set<User.Role> ALLOWED = Collections.unmodifiableSet(Utils.set(User.Role.ADMIN, User.Role.NORMAL_USER, User.Role.DESCRIPTION_WRITER));
	
	@Override
	protected Set<User.Role> allowedRoles() {
		return ALLOWED;
	}
	
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
		Document response =  getTaxonomyDAO()
				.search(new TaxonSearch(searchword, 30, checklist)
						.setOnlySpecies(onlySpecies)
						.setOnlyFinnish(onlyFinnish))
				.getResultsAsDocument();
		responseData.setData("response", response);
		responseData.setData("taxonpageBaseLinkURL", req.getParameter("taxonpageBaseLinkURL"));
		return responseData;
	}

}
