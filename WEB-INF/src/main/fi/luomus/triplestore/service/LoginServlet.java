package fi.luomus.triplestore.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.utils.LoginUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/login/*"})
public class LoginServlet extends EditorBaseServlet {

	private static final long serialVersionUID = -129025925633149672L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return LoginUtil.authorized();
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return new LoginUtil(frontpage(), getConfig(), getSession(req), super.initResponseData(req), getErrorReporter()).processGet(req);
	}

	private String frontpage() {
		return getConfig().baseURL() + "/greet";
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return new LoginUtil(frontpage(), getConfig(), getSession(req), super.initResponseData(req), getErrorReporter()).processPost(req);
	}

}
