package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/editor/validate/*"})
public class TriplestoreEditorValidationServlet extends TriplestoreEditorServlet {

	private static final long serialVersionUID = 3033247276619037565L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String qname = getQname(req);
		try {
			TriplestoreDAO dao = getTriplestoreDAO();
			Model model = dao.get(qname);
			JSONObject data = new JSONObject(req.getReader().readLine());
			model = toModel(qname, data);
			JSONObject validationResponse = validate(model, dao);
			return jsonResponse(validationResponse);
		} catch (Exception e) {
			JSONObject validationResponse = new JSONObject();
			validationResponse.setBoolean("hasErrors", true);
			validationResponse.getArray("errors").appendString("Validations failed for unknown reason! " + LogUtils.buildStackTrace(e, 5));
			return jsonResponse(validationResponse);
		}
	}

}
