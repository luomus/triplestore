package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;

@WebServlet(urlPatterns = {"/taxonomy-editor/not-found"})
public class PageNotFoundServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -2614636879272477024L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return initResponseData(req).setViewName("pageNotFound");
	}

}
