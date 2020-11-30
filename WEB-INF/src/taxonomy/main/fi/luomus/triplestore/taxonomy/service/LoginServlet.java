package fi.luomus.triplestore.taxonomy.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.utils.LoginUtil;

@WebServlet(urlPatterns = {"/taxonomy-editor/login/*"})
public class LoginServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -6030911036269793172L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	private LoginUtil util = null;

	private LoginUtil getLoginUtil() {
		if (util == null) {
			util = new LoginUtil(
					frontpages(),
					getConfig(), getErrorReporter(),
					Utils.set("MA.admin", "MA.taxonEditorUser", "MA.taxonEditorUserDescriptionWriterOnly"));
		}
		return util;
	}

	private Map<String, String> frontpages() {
		Map<String, String> frontPages = new HashMap<>();
		frontPages.put("MA.admin", getConfig().baseURL());
		frontPages.put("MA.taxonEditorUser", getConfig().baseURL());
		frontPages.put("MA.taxonEditorUserDescriptionWriterOnly", getConfig().baseURL()+"/taxon-descriptions");
		return frontPages;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return getLoginUtil().processGet(req, getSession(req), super.initResponseData(req, false));
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return getLoginUtil().processPost(req, getSession(req), super.initResponseData(req, false));
	}

}
