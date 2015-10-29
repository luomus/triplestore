package fi.luomus.triplestore.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.CreatableResource;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/editor/create/*"})
public class TriplestoreEditorCreateNew extends TriplestoreEditorServlet {

	private static final long serialVersionUID = -7948368282424564525L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return getUser(req).isAdmin();
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String qnamePrefix = getQname(req);
		CreatableResource creatableResource = validPrefix(qnamePrefix);
		
		if (creatableResource == null) {
			getSession(req).setFlashError("Invalid prefix");
			return redirectTo(getConfig().baseURL()+"/editor", res);
		}
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		Qname qname = dao.getSeqNextValAndAddResource(qnamePrefix);
		Model model = new Model(qname);
		model.setType(creatableResource.getType());
		dao.store(model);
		
		getSession(req).setFlashSuccess("Created!");
		return redirectToGet(qname.toString());
	}

	private CreatableResource validPrefix(String qnamePrefix) {
		for (CreatableResource c : CREATABLE_RESOURCES) {
			if (c.getNamespacePrefix().equals(qnamePrefix)) {
				return c;
			}
		}
		return null;
	}

}
