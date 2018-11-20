package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/addchild/*"})
public class ApiAddChildServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7393608674235660598L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req).setViewName("api-addchild");
		
		String checklistQname = req.getParameter("checklist");
		String parentQname = req.getParameter("parent").replace("MX", "MX.");
		String scientificName = req.getParameter("scientificName");
		String author = req.getParameter("author");
		String rank = req.getParameter("taxonRank");
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		EditableTaxon parent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(parentQname));
		checkPermissionsToAlterTaxon(parent, req);

		EditableTaxon taxon = createTaxon(scientificName, taxonomyDAO);
		taxon.setChecklist(new Qname(checklistQname)); 
		taxon.setParentQname(parent.getQname()); 
		taxon.setScientificNameAuthorship(author);
		if (given(rank)) {
			taxon.setTaxonRank(new Qname(rank));
		}
		
		dao.addTaxon(taxon);
		
		taxon.invalidateSelfAndLinking();
		
		responseData.setData("newTaxon", taxon);
		
		ValidationData validationData = new TaxonValidator(dao, taxonomyDAO, getErrorReporter()).validate(taxon);
		responseData.setData("validationResults", validationData);
		
		return responseData;
	}

	
}
