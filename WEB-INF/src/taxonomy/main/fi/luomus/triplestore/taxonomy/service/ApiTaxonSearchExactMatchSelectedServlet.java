package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.xml.Document;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonomy-search-exact-match-selected/*"})
public class ApiTaxonSearchExactMatchSelectedServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -1714953463892118021L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String selectedTaxon = req.getParameter("selectedTaxonName");
		String checklist = req.getParameter("checklist");
		if (!given(selectedTaxon) || !given(checklist)) {
			return noExactMatchFound(res);
		}
		Document response =  getTaxonomyDAO().search(selectedTaxon, checklist);
		if (!response.getRootNode().hasChildNodes("exactMatch")) {
			return noExactMatchFound(res);
		}
		if (response.getRootNode().getNode("exactMatch").getChildNodes().size() != 1) {
			return noExactMatchFound(res);
		}
		String qname = response.getRootNode().getNode("exactMatch").getChildNodes().get(0).getName();
		return exactMatch(qname, res);
	}

	private ResponseData exactMatch(String qname, HttpServletResponse res) throws Exception {
		return jsonResponse("{\"exactMatch\": true, \"qname\": \""+qname+"\"}", res);
	}

	private ResponseData noExactMatchFound(HttpServletResponse res) throws Exception {
		return jsonResponse("{\"exactMatch\": false}", res);
	}
	
}
