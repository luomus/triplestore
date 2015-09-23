package fi.luomus.triplestore.service;

import fi.luomus.commons.services.ResponseData;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/error/*", "/taxonomy-editor/error/*"})
public class ErrorPageServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 4357120355162822776L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return false;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return super.initResponseData(req).setViewName("error");
	}
	
}
