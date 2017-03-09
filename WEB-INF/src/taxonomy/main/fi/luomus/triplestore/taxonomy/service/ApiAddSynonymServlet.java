package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

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
		
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon synonymParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(synonymParentQname));
		checkPermissionsToAlterTaxon(synonymParent, req);
		
		EditableTaxon taxon = createTaxon(scientificName, taxonomyDAO);
		taxon.setScientificNameAuthorship(author);
		if (given(rank)) {
			taxon.setTaxonRank(new Qname(rank));
		}
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		if (!given(synonymParent.getTaxonConceptQname())) { // shouldn't be any
			Qname taxonConcept = dao.addTaxonConcept();
			dao.store(new Subject(synonymParent.getQname()), new Statement(new Predicate("MX.circumscription"), new ObjectResource(taxonConcept)));
			synonymParent.setTaxonConceptQname(taxonConcept);
		}
		taxon.setTaxonConceptQname(synonymParent.getTaxonConceptQname());
		dao.addTaxon(taxon);
		synonymParent.invalidate();
		
		return responseData.setData("synonymTaxon", taxon);
	}
	
}
