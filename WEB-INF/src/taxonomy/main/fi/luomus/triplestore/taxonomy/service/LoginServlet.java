package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.utils.LoginUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/login/*"})
public class LoginServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -6030911036269793172L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return LoginUtil.authorized();
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return new LoginUtil(frontpage(), getConfig(), getSession(req), super.initResponseData(req), getErrorReporter(), getTriplestoreDAO()).processGet(req);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return new LoginUtil(frontpage(), getConfig(), getSession(req), super.initResponseData(req), getErrorReporter(), getTriplestoreDAO()).processPost(req);
	}

	private String frontpage() {
		return getConfig().baseURL();
	}
	
}
