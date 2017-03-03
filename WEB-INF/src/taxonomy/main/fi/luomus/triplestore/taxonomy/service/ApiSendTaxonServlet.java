package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/sendtaxon/*"})
public class ApiSendTaxonServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7714464443111669323L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		
		String taxonToSendID = req.getParameter("taxonToSendID").replace("MX", "MX.");
		String newParentID = req.getParameter("newParentID");
		
		System.out.println(taxonToSendID + " -> " + newParentID);
		if ("foobar".equals("foobar")) {
			return apiSuccessResponse(res);
		}
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon toSend = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonToSendID));
		EditableTaxon newParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newParentID));
		
		checkPermissionsToAlterTaxon(toSend, req);
		checkPermissionsToAlterTaxon(newParent, req);
		
		toSend.invalidate();
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		dao.store(new Subject(taxonToSendID), new Statement(new Predicate("MX.isPartOf"), new ObjectResource(newParentID)));
		
		toSend.invalidate();
		newParent.invalidate();
		
		return apiSuccessResponse(res);
	}
	
}
