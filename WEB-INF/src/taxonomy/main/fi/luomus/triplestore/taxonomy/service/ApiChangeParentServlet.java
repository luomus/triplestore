package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.lajitietokeskus.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/changeparent/*"})
public class ApiChangeParentServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 9165675894617551000L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = req.getParameter("taxon").replace("MX", "MX.");
		String newParentQname = req.getParameter("newParent").replace("MX", "MX.");
		
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		Qname oldParent = taxon.getParentQname();
		
		try {
			checkPermissionsToAlterTaxon(taxon, req);
			checkPermissionsToAlterTaxon(newParentQname, req);
		} catch (IllegalAccessException noAccess) {
			return new ResponseData().setData("error", noAccess.getMessage()).setViewName("api-error");
			
		}
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		dao.store(new Subject(taxonQname), new Statement(new Predicate("MX.isPartOf"), new ObjectResource(newParentQname)));
		
		taxonomyDAO.invalidateTaxon(taxon);
		taxonomyDAO.invalidateTaxon(new Qname(newParentQname));
		taxonomyDAO.invalidateTaxon(oldParent);
		
		return apiSuccessResponse(res);
	}
	
}
