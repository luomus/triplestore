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
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/setAsSynonym/*"})
public class ApiSetAsSynonymServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 4325161753479424734L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = req.getParameter("taxon").replace("MX", "MX.");
		String newSynonymParentQname = req.getParameter("newSynonymParent").replace("MX", "MX.");

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		EditableTaxon newSynonymParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newSynonymParentQname));
		Qname oldParent = taxon.getParentQname();

		try {
			checkPermissionsToAlterTaxon(taxon, req);
			checkPermissionsToAlterTaxon(newSynonymParent, req);
		} catch (IllegalAccessException noAccess) {
			return new ResponseData().setData("error", noAccess.getMessage()).setViewName("api-error");

		}

		if (taxon.getChecklist() != null && !taxon.getSynonymTaxons().isEmpty()) {
			return new ResponseData().setData("error", "Can not switch taxon to be a synonym while it itself has synonyms. Move the synonyms of this taxon first.").setViewName("api-error");
		}

		taxon.invalidate();
		newSynonymParent.invalidate();
		if (given(oldParent)) {
			((EditableTaxon) taxonomyDAO.getTaxon(oldParent)).invalidate();
		}

		TriplestoreDAO dao = getTriplestoreDAO(req);

		if (!given(newSynonymParent.getTaxonConceptQname())) { // All taxons should have taxon concept, but just in case the parent doesn't have we create it
			Qname taxonConcept = dao.addTaxonConcept();
			changeTaxonConcept(newSynonymParent, taxonConcept, dao);
			newSynonymParent.setTaxonConceptQname(taxonConcept);
		}

		Qname newTaxonConcept = newSynonymParent.getTaxonConceptQname();

		changeTaxonConcept(taxon, newTaxonConcept, dao);
		dao.delete(new Subject(taxon.getQname()), new Predicate("MX.isPartOf"));
		dao.delete(new Subject(taxon.getQname()), new Predicate("MX.nameAccordingTo"));

		return apiSuccessResponse(res);
	}


	private void changeTaxonConcept(Taxon taxon, Qname concept, TriplestoreDAO dao) throws Exception {
		dao.store(new Subject(taxon.getQname()), new Statement(new Predicate("MX.circumscription"), new ObjectResource(concept)));
	}

}
