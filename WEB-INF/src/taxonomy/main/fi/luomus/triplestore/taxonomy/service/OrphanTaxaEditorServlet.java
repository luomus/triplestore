package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/orphan/*"})
public class OrphanTaxaEditorServlet extends TaxonomyTreesEditorServlet {

	private static final long serialVersionUID = 5799638997493078695L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("orphan");
		return responseData;
	}
	
}
