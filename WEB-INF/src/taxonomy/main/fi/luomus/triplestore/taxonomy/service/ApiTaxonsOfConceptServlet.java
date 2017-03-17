package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonConcept;
import fi.luomus.commons.taxonomy.TaxonomyDAO;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxaOfConcept/*"})
public class ApiTaxonsOfConceptServlet extends ApiBaseServlet {
	
	private static final long serialVersionUID = 6456306430302411448L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-taxaOfConcept");
		
		TaxonomyDAO dao = getTaxonomyDAO();
		
		String conceptQname = getQname(req);
		if (!given(conceptQname) || conceptQname.equals("undefined")) {
			return redirectTo500(res);
		} 
		if (!conceptQname.contains(".")) {
			conceptQname = conceptQname.replace("MC", "MC.");
		}
		
		TaxonConcept c = dao.getTaxonContainer().getTaxonConcept(new Qname(conceptQname));
		Collection<Taxon> taxa = new ArrayList<>();
		for (Qname taxonId : c.getTaxonsPartOfConcept()) {
			taxa.add(dao.getTaxon(taxonId));
		}
		
		responseData.setData("taxa", taxa);
		return responseData;
	}
	
}
