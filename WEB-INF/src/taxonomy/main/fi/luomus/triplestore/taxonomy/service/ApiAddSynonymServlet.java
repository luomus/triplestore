package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/addsynonym/*"})
public class ApiAddSynonymServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7393608674235660598L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-addsynonym");
		
		String synonymParentQname = req.getParameter("synonymOf").replace("MX", "MX.");
		String scientificName = req.getParameter("scientificName");
		String author = req.getParameter("author");
		String rank = req.getParameter("taxonRank");
		
		EditableTaxon synonymParent = (EditableTaxon) getTaxonomyDAO().getTaxon(new Qname(synonymParentQname));
		
		checkPermissionsToAlterTaxon(synonymParent, req);
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		Taxon taxon = createTaxonAndFetchNextId(scientificName, dao);
		taxon.setScientificNameAuthorship(author);
		if (given(rank)) {
			taxon.setTaxonRank(new Qname(rank));
		}
				
		if (!given(synonymParent.getTaxonConcept())) {
			Qname taxonConcept = dao.addTaxonConcept();
			dao.store(new Subject(synonymParent.getQname()), new Statement(new Predicate("MX.circumscription"), new ObjectResource(taxonConcept)));
			synonymParent.setTaxonConcept(taxonConcept);
		}
		
		taxon.setTaxonConcept(synonymParent.getTaxonConcept());
						
		dao.addTaxon(taxon);
		
		getTaxonomyDAO().invalidateTaxon(synonymParent.getQname());
		
		responseData.setData("synonymTaxon", taxon);
		
		return responseData;
	}
	
}
