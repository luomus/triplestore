package fi.luomus.triplestore.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.XMLWriter;

@WebServlet(urlPatterns = {"/uri/*"})
public class UriGeneratorServlet extends EditorBaseServlet {

	private static final long serialVersionUID = -5722253885854177546L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return processPost(req, res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return processPost(req, res);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Format format = getFormat(req);
		if (format == DEFAULT_FORMAT) {
			format = Format.JSON;
		}

		String namespace = getQname(req);

		if (!given(namespace) || namespace.contains(":")) {
			return status500(res);
		}

		Qname next = getTriplestoreDAO().getSeqNextValAndAddResource(namespace);

		Document response = new Document("response");
		response.getRootNode().addAttribute("uri", next.toURI());
		response.getRootNode().addAttribute("qname", next.toString());

		if (format == Format.XML) {
			return xmlResponse(response, res);
		}
		if (format == Format.JSON) {
			String xml = new XMLWriter(response).generateXML();
			JSONObject jsonObject = XML.toJSONObject(xml);
			String json = jsonObject.toString();
			return jsonResponse(json, res);
		}
		throw new UnsupportedOperationException("Not yet implemented for format: " + format.toString());
	}

}
