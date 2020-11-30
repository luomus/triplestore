package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/editor/new-predicate/*"})
public class TriplestoreEditorNewPredicateServlet extends TriplestoreEditorServlet {

	private static final long serialVersionUID = -4750614854155973416L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String qname = getQname(req);
		ResponseData responseData = initResponseData(req, false).setViewName("new-predicate");
		try {
			TriplestoreDAO dao = getTriplestoreDAO();
			if (!dao.resourceExists(qname)) {
				return responseData.setData("error", "Predicate does not exist.");
			}
			RdfProperty property = dao.getProperty(new Predicate(qname));
			return responseData.setData("property", property);
		} catch (Exception e) {
			return responseData.setData("error", e.getMessage());
		}
	}

}
