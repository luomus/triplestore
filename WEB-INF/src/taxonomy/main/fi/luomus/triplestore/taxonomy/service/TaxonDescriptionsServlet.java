package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/taxon-descriptions/*"})
public class TaxonDescriptionsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -2281661002076649983L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			return redirectTo500(res);
		} 

		Taxon taxon = getTaxonomyDAO().getTaxon(new Qname(taxonQname));

		return responseData.setViewName("taxon-descriptions").setData("taxon", taxon).setData("root", taxon);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo("/", res);
	}

}
