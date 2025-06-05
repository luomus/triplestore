package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/editor/create-custom/*"})
public class TriplestoreEditorCreateCustom extends TriplestoreEditorServlet {

	private static final long serialVersionUID = -1973064571468172463L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return getUser(req).isAdmin();
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		Qname qname = new Qname(getQname(req));

		if (!qname.isSet()) {
			getSession(req).setFlashError("Invalid resource name");
			return redirectTo(getConfig().baseURL()+"/editor");
		}

		if (!CREATABLE_NAMESPACES.contains(qname.getNamespace())) {
			getSession(req).setFlashError("Can not create to this namespace");
			return redirectTo(getConfig().baseURL()+"/editor");
		}


		TriplestoreDAO dao = getTriplestoreDAO(req);
		if (dao.resourceExists(qname)) {
			getSession(req).setFlashError("Resource already exists");
			return redirectTo(getConfig().baseURL()+"/editor");
		}

		dao.addResource(qname);

		getSession(req).setFlashSuccess("Created!");
		return redirectToGet(qname.toString());
	}

}
