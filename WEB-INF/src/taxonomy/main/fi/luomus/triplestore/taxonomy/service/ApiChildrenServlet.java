package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/children/*"})
public class ApiChildrenServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 1842696365544588842L;
	
	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-children");
		
		TaxonomyDAO dao = getTaxonomyDAO();
		
		String parentQname = getQname(req);
		if (!given(parentQname) || parentQname.equals("undefined")) {
			return redirectTo500(res);
		} 
		parentQname = parentQname.replace("MX", "MX.");
		
		Taxon parent = dao.getTaxon(new Qname(parentQname));
		responseData.setData("parent", parent);
		responseData.setData("children", parent.getChildren());
		return responseData;
	}
	
}
