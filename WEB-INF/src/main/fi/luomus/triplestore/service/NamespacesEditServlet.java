package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.NamespacesDAO;
import fi.luomus.triplestore.dao.NamespacesDAOImple;
import fi.luomus.triplestore.models.Namespace;

@WebServlet(urlPatterns = {"/namespaces-edit/*"})
public class NamespacesEditServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 4107748303026114116L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return getUser(req).isAdmin();
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = super.initResponseData(req, false).setViewName("namespaces-edit");
		responseData.setData("namespaces", getDao().getNamespaces());
		return responseData;
	}


	private NamespacesDAO getDao() {
		return new NamespacesDAOImple(getConfig());
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Namespace namespace = new Namespace(
				req.getParameter("namespace"),
				req.getParameter("personInCharge"),
				req.getParameter("purpose"),
				req.getParameter("type"),
				req.getParameter("qnamePrefix"));
		getDao().upsert(namespace);
		return new ResponseData().setRedirectLocation(getConfig().baseURL()+"/namespaces-edit");
	}

}