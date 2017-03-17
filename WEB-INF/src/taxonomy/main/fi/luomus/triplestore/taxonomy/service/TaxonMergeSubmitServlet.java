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

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/merge/*"})
public class TaxonMergeSubmitServlet extends TaxonomyEditorBaseServlet {

	private static final String NEW_TAXON_SCIENTIFIC_NAME = "newTaxonScientificName";
	private static final long serialVersionUID = 1949494781537607919L;
	private static final Predicate MC_INCLUDED_IN_PREDICATE = new Predicate("MC.includedIn");
	private static final Predicate NAME_ACCORDING_TO_PREDICATE = new Predicate("MX.nameAccordingTo");
	private static final Predicate IS_PART_OF_PREDICATE = new Predicate("MX.isPartOf");


	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		Qname rootTaxonId = new Qname(req.getParameter("rootTaxonId"));
		Collection<EditableTaxon> toMerge = null;
		try {
			toMerge = parseToMerge(req, taxonomyDAO);
		} catch (Exception e) {
			return errorPage("One of the taxa to merge does not exist!", req, res, null, rootTaxonId);
		}

		if (toMerge.size() < 2 || !given(rootTaxonId)) {
			return errorPage("Must select at least one other taxon to merge with the taxon.", req, res, toMerge, rootTaxonId);
		}

		if (!given(req.getParameter(NEW_TAXON_SCIENTIFIC_NAME))) {
			return errorPage("Must give scientific name of the taxon that the other taxa are merged to", req, res, toMerge, rootTaxonId);
		}
		
		for (EditableTaxon taxonToMerge : toMerge) {
			try {
				checkPermissionsToAlterTaxon(taxonToMerge, req);
			} catch (Exception e) {
				return errorPage("You do not have permissions to merge taxon " + info(taxonToMerge), req, res, toMerge, rootTaxonId);
			}
			if (taxonToMerge.hasCriticalData()) {
				return errorPage("Taxon " + info(taxonToMerge) + " has critical data and can not be merged", req, res, toMerge, rootTaxonId);
			}
			taxonToMerge.invalidate();
		}
		
		TriplestoreDAO dao = getTriplestoreDAO(req);

		EditableTaxon newTaxon = parseAndCreateNewTaxon(req, dao, taxonomyDAO, toMerge.iterator().next());

		for (EditableTaxon taxonToMerge : toMerge) {
			dao.delete(new Subject(taxonToMerge.getQname()), IS_PART_OF_PREDICATE);
			dao.delete(new Subject(taxonToMerge.getQname()), NAME_ACCORDING_TO_PREDICATE);
			dao.store(new Subject(taxonToMerge.getTaxonConceptQname()), new Statement(MC_INCLUDED_IN_PREDICATE, new ObjectResource(newTaxon.getTaxonConceptQname())));
		}

		taxonomyDAO.clearTaxonConceptLinkings();

		return redirectToTree(res, toMerge, rootTaxonId);
	}

	private EditableTaxon parseAndCreateNewTaxon(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon taxonToMerge) throws Exception {
		EditableTaxon taxon = taxonomyDAO.createTaxon();
		taxon.setScientificName(req.getParameter(NEW_TAXON_SCIENTIFIC_NAME));
		taxon.setScientificNameAuthorship(req.getParameter("newTaxonAuthors"));
		String rank = req.getParameter("newTaxonRank");
		if (given(rank)) {
			taxon.setTaxonRank(new Qname(rank));
		}
		
		taxon.setChecklist(taxonToMerge.getChecklist());
		taxon.setParentQname(taxonToMerge.getParentQname());
		taxon.setTaxonConceptQname(dao.addTaxonConcept());
		dao.addTaxon(taxon);
		return taxon;
	}

	private String info(EditableTaxon taxonToMerge) {
		if (given(taxonToMerge.getScientificName())) {
			return taxonToMerge.getScientificName() + " (" + taxonToMerge.getQname() + ")";
		}
		return taxonToMerge.getQname().toString();
	}

	private ResponseData errorPage(String errorMessage, HttpServletRequest req, HttpServletResponse res, Collection<EditableTaxon> taxonToMerge, Qname rootTaxonId) {
		getSession(req).setFlashError(errorMessage);
		return redirectToTree(res, taxonToMerge, rootTaxonId);
	}

	private ResponseData redirectToTree(HttpServletResponse res, Collection<EditableTaxon> taxonToMerge, Qname rootTaxonId) {
		if (taxonToMerge != null) {
			for (EditableTaxon t : taxonToMerge) {
				if (t.getQname().equals(rootTaxonId)) {
					rootTaxonId = TaxonomyTreesEditorServlet.DEFAULT_ROOT_QNAME;
					break;
				}
			}
		}
		return redirectTo(getConfig().baseURL() + "/" + rootTaxonId, res);
	}

	private Collection<EditableTaxon> parseToMerge(HttpServletRequest req, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		Collection<EditableTaxon> toMerge = new HashSet<>();
		for (String qname : req.getParameterValues("taxonToMergeId")) {
			if (!qname.contains(".")) {
				qname = qname.replace("MX", "MX.");
			}
			toMerge.add((EditableTaxon) taxonomyDAO.getTaxon(new Qname(qname)));
		}
		return toMerge;
	}

}
