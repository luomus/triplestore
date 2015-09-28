package fi.luomus.triplestore.service;

import fi.luomus.commons.services.ResponseData;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/editor/invalidate-caches/*"})
public class InvalidateCachesServlet extends TriplestoreEditorServlet {

	private static final long serialVersionUID = -9033088120799363722L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		getTriplestoreDAO(req).clearCaches();
		return super.processGet(req, res).setData("success", "Caches cleared.");
	}

}
