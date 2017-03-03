package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.LogUtils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ApiBaseServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -4073999205890778398L;

	@Override
	protected ResponseData notAuthorizedRequest(HttpServletRequest req, HttpServletResponse res) {
		return redirectTo403(res);
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo500(res);
	}
	
	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo500(res);
	}
	
	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo500(res);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo500(res);
	}
	
	@Override
	protected void handleException(Exception e, HttpServletRequest req, HttpServletResponse res) throws ServletException {
		getSession(req).setFlash("<pre>" + e.getMessage() + "<br/>" + LogUtils.buildStackTrace(e)+"</pre>");
		throw new ServletException(e);
	}
	
	protected ResponseData apiSuccessResponse(HttpServletResponse res) throws IOException {
		res.setContentType("text/plain");
		PrintWriter out = res.getWriter();
		out.print("ok");
		out.flush();
		return new ResponseData().setOutputAlreadyPrinted();
	}

	protected ResponseData apiErrorResponse(String error, HttpServletResponse res) throws IOException {
		res.setContentType("text/plain");
		res.setStatus(400);
		PrintWriter out = res.getWriter();
		out.print(error);
		out.flush();
		return new ResponseData().setOutputAlreadyPrinted();
	}
}
