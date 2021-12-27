package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.NamespacesDAO;
import fi.luomus.triplestore.dao.NamespacesDAOImple;

@WebServlet(urlPatterns = {"/namespaces/*"})
public class NamespacesServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 4107748303026114116L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = super.initResponseData(req, false).setViewName("namespaces");
		responseData.setData("namespaces", getDao().getNamespaces());
		return responseData;
	}


	private NamespacesDAO getDao() {
		return new NamespacesDAOImple(getConfig());
	}

}