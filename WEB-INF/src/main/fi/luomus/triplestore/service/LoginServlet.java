package fi.luomus.triplestore.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.utils.LoginUtil;

@WebServlet(urlPatterns = {"/login/*"})
public class LoginServlet extends EditorBaseServlet {

	private static final long serialVersionUID = -129025925633149672L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	private LoginUtil util = null;

	private LoginUtil getLoginUtil() {
		if (util == null) {
			util = new LoginUtil(frontpagesForRoles(), getConfig(), getErrorReporter(), Utils.set("MA.admin"));
		}
		return util; 
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return getLoginUtil().processGet(req, getSession(req), super.initResponseData(req));
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return getLoginUtil().processPost(req, getSession(req), super.initResponseData(req));
	}

	private Map<String, String> frontpagesForRoles() {
		Map<String, String> frontpages = new HashMap<>();
		frontpages.put("MA.admin", getConfig().baseURL() + "/greet");
		return frontpages;
	}

}
