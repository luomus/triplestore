package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;

@WebServlet(urlPatterns = {"/greet/*"})
public class GreetServlet extends EditorBaseServlet {

	private static final long serialVersionUID = -1861718677712021240L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (req.getParameter("exceptiontest") != null) throw new Exception("Exception test.");
		return super.initResponseData(req, true).setViewName("greet");
	}
}
