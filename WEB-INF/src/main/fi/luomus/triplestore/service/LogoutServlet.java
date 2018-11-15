package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;

@WebServlet(urlPatterns = {"/logout/*"})
public class LogoutServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 7182521228588132119L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}
	
	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		SessionHandler sessionHandler = getSession(req);
		if (sessionHandler.hasSession()) {
			sessionHandler.removeAuthentication("triplestore");
		}
		return redirectTo(getConfig().baseURL()+"/login");
	}

}
