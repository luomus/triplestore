package fi.luomus.triplestore.taxonomy.service;

import java.util.Collection;

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

@WebServlet(urlPatterns = {"/taxonomy-editor/split/*"})
public class TaxonSplitSubmitServlet extends TaxonomyEditorBaseServlet {

	private static final Predicate MC_INCLUDED_IN_PREDICATE = new Predicate("MC.includedIn");
	private static final Predicate NAME_ACCORDING_TO_PREDICATE = new Predicate("MX.nameAccordingTo");
	private static final Predicate IS_PART_OF_PREDICATE = new Predicate("MX.isPartOf");
	private static final long serialVersionUID = 6726551370591523603L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname taxonToSplitID = new Qname(req.getParameter("taxonToSplitID").replace("MX", "MX."));
		Qname rootTaxonId = new Qname(req.getParameter("rootTaxonId"));
		if (!given(taxonToSplitID) || !given(rootTaxonId)) redirectTo500(res);
		
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxonToSplit = (EditableTaxon) taxonomyDAO.getTaxon(taxonToSplitID);
		checkPermissionsToAlterTaxon(taxonToSplit, req);
		
		if (taxonToSplit.hasCriticalData()) {
			throw new IllegalStateException("Taxon " + taxonToSplitID + " has critical data and can not be splitted!");
		}
		
		TriplestoreDAO dao = getTriplestoreDAO(req);

		Collection<EditableTaxon> newTaxons = parseAndCreateNewTaxons(req, dao, taxonomyDAO);
		for (EditableTaxon t : newTaxons) {
			t.setChecklist(taxonToSplit.getChecklist());
			t.setParentQname(taxonToSplit.getParentQname());
		}
		
		taxonToSplit.invalidate();
		dao.delete(new Subject(taxonToSplitID), IS_PART_OF_PREDICATE);
		dao.delete(new Subject(taxonToSplitID), NAME_ACCORDING_TO_PREDICATE);
		
		if (notGiven(taxonToSplit.getTaxonConceptQname())) {
			taxonToSplit.setTaxonConceptQname(dao.addTaxonConcept());
		}
		
		Qname splittedConcept = taxonToSplit.getTaxonConceptQname();
		for (EditableTaxon newTaxon : newTaxons) {
			Qname newTaxonConcept = newTaxon.getTaxonConceptQname();
			dao.store(new Subject(newTaxonConcept), new Statement(MC_INCLUDED_IN_PREDICATE, new ObjectResource(splittedConcept)));
		}
		
		taxonomyDAO.clearTaxonConceptLinkings();
		
		return redirectToTree(res, taxonToSplitID, rootTaxonId);
	}

	private boolean notGiven(Qname taxonConceptQname) {
		return taxonConceptQname == null || !taxonConceptQname.isSet();
	}

	private ResponseData redirectToTree(HttpServletResponse res, Qname taxonToSplitID, Qname rootTaxonId) {
		if (taxonToSplitID.equals(rootTaxonId)) {
			rootTaxonId = TaxonomyTreesEditorServlet.DEFAULT_ROOT_QNAME;
		}
		return redirectTo(getConfig().baseURL() + "/" + rootTaxonId, res);
	}

}
