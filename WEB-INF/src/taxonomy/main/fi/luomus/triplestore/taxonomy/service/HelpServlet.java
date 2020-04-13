package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;

@WebServlet(urlPatterns = {"/taxonomy-editor/help/*"})
public class HelpServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 692976076703766316L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req);
		return responseData.setViewName("help");
	}
	
}
