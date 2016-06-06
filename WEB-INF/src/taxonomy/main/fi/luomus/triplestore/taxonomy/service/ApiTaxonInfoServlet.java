package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonInfo/*"})
public class ApiTaxonInfoServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 8505283167260522117L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = getId(req);
		Taxon taxon = getTaxonomyDAO().getTaxon(new Qname(taxonQname));
		if (taxon == null) return redirectTo404(res);
		
		JSONObject responseObject = new JSONObject();
		responseObject.setString("scientificName", taxon.getScientificName());
		responseObject.getObject("vernacularName").setString("fi", taxon.getVernacularName("fi"));
		
		return jsonResponse(responseObject, res);
	}

}
