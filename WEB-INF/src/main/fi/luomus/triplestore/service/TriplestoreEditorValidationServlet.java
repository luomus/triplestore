package fi.luomus.triplestore.service;

import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.Model;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
			return jsonResponse(validationResponse, res);
		} catch (Exception e) {
			JSONObject validationResponse = new JSONObject();
			validationResponse.setBoolean("hasErrors", true);
			validationResponse.getArray("errors").appendString("Validations failed for unknown reason! " + LogUtils.buildStackTrace(e, 5));
			return jsonResponse(validationResponse, res);
		}
	}

}
