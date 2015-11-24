package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/logout/*"})
public class LogoutServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -7422002943924712411L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		SessionHandler sessionHandler = getSession(req);
		if (sessionHandler.hasSession()) {
			sessionHandler.invalidate();
		}
		return redirectTo(getConfig().baseURL()+"/login", res);
	}

}
