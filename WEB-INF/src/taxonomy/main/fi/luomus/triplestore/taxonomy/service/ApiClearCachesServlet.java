package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/clear-caches/*"})
public class ApiClearCachesServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 8622322469725961760L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins");
		getTaxonomyDAO().clearCaches();
		getTriplestoreDAO(req).clearCaches();
		return apiSuccessResponse(res);
	}
	
}
