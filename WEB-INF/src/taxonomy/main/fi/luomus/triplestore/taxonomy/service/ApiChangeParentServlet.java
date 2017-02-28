package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/changeparent/*"})
public class ApiChangeParentServlet extends ApiBaseServlet {

	private static final String LAST_IN_ORDER = String.valueOf(Integer.MAX_VALUE);
	private static final long serialVersionUID = 9165675894617551000L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = req.getParameter("taxon").replace("MX", "MX.");
		String newParentQname = req.getParameter("newParent").replace("MX", "MX.");

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		EditableTaxon oldParent = taxon.hasParent() ? (EditableTaxon) taxonomyDAO.getTaxon(taxon.getParentQname()) : null;
		EditableTaxon newParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newParentQname));

		try {
			checkPermissionsToAlterTaxon(taxon, req);
			checkPermissionsToAlterTaxon(newParent, req);
			if (oldParent != null) {
				checkPermissionsToAlterTaxon(oldParent, req);
			}
		} catch (IllegalAccessException noAccess) {
			return new ResponseData().setData("error", noAccess.getMessage()).setViewName("api-error");

		}

		if (taxon.getChecklist() != null && !taxon.getChecklist().equals(newParent.getChecklist())) {
			throw new IllegalAccessException("Can not move a taxon from some other checklist to be a child of another checklist");
		}

		taxon.invalidate();
		newParent.invalidate();
		if (oldParent != null) {
			oldParent.invalidate();
		}

		TriplestoreDAO dao = getTriplestoreDAO(req);
		dao.store(new Subject(taxonQname), new Statement(new Predicate("MX.isPartOf"), new ObjectResource(newParentQname)));
		dao.store(new Subject(taxonQname), new Statement(new Predicate("sortOrder"), new ObjectLiteral(LAST_IN_ORDER)));
		if (taxon.getChecklist() == null) {
			dao.store(new Subject(taxonQname), new Statement(new Predicate("MX.nameAccordingTo"), new ObjectResource(newParent.getChecklist())));
			Qname newTaxonConcept = dao.addTaxonConcept();
			dao.store(new Subject(taxonQname), new Statement(new Predicate("MX.circumscription"), new ObjectResource(newTaxonConcept)));
		}

		return apiSuccessResponse(res);
	}

}
