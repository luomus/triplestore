package fi.luomus.triplestore.taxonomy.service;

import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.TaxonConcept;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.service.ApiAddSynonymServlet.SynonymType;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/removeSynonym/*"})
public class ApiRemoveSynonymServlet extends ApiBaseServlet {

	private static final String REMOVED_ID_PARAMETER = "removedId";
	private static final long serialVersionUID = 7393608674235660598L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		SynonymType synonymType = ApiAddSynonymServlet.getSynonymType(req);
		Qname removedId = getRemovedId(req);
		EditableTaxon synonymParent = ApiAddSynonymServlet.getSynonymParent(req, dao, taxonomyDAO);
				
		if (synonymType == SynonymType.SYNONYM) {
			removeSynonym(dao, taxonomyDAO, removedId);
		} else if (synonymType == SynonymType.MISAPPLIED) {
			removeMisapplied(dao, taxonomyDAO, synonymParent, removedId);
		} else if (synonymType == SynonymType.UNCERTAIN) {
			removeUncertain(dao, taxonomyDAO, synonymParent, removedId);
		} else if (synonymType == SynonymType.MISSPELLED) {
			removeMisspelled(dao, taxonomyDAO, synonymParent, removedId);
		} else if (synonymType == SynonymType.INCLUDES) {
			removeIncludes(dao, synonymParent, removedId);
		} else if (synonymType == SynonymType.INCLUDED_IN) {
			removeIncludedIn(dao, synonymParent, removedId);
		} else if (synonymType == SynonymType.BASIONYM) {
			removeBasionym(dao, taxonomyDAO, synonymParent, removedId);
		} else {
			throw new UnsupportedOperationException("Unknown synonym type: "  + synonymType);
		}
		taxonomyDAO.clearTaxonConceptLinkings();	
		return apiSuccessResponse(res);
	}

	private void removeSynonym(TriplestoreDAO dao, TaxonomyDAO taxonomyDAO, Qname removedSynonymTaxonId) throws Exception {
		TaxonConcept oldConcept = taxonomyDAO.getTaxon(removedSynonymTaxonId).getTaxonConcept();
		Collection<Qname> includingConcepts = oldConcept.getIncludingConcepts();
		Collection<Qname> includedConcepts = oldConcept.getIncludedConcepts();
		
		Qname newConcept = dao.addTaxonConcept();
		dao.store(new Subject(removedSynonymTaxonId), new Statement(ApiAddSynonymServlet.CIRCUMSCRIPTION, new ObjectResource(newConcept)));
		
		for (Qname includedIn : includingConcepts) {
			Model model = dao.get(newConcept);
			model.addStatement(new Statement(ApiAddSynonymServlet.INCLUDED_IN, new ObjectResource(includedIn)));
			dao.store(model);
		}
		
		for (Qname included : includedConcepts) {
			Model model = dao.get(included);
			model.addStatement(new Statement(ApiAddSynonymServlet.INCLUDED_IN, new ObjectResource(newConcept)));
			dao.store(model);
		}
	}

	private void removeMisapplied(TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon synonymParent, Qname removedSynonymTaxonId) throws Exception {
		Qname subject = removedSynonymTaxonId;
		Predicate predicate = ApiAddSynonymServlet.MISAPPLIED_CIRCUMSCRIPTION;
		Qname object = synonymParent.getTaxonConceptQname();
		deleteStatement(dao, subject, predicate, object);

		EditableTaxon removedTaxon = (EditableTaxon) taxonomyDAO.getTaxon(removedSynonymTaxonId);
		removedTaxon.invalidate();
	}

	private void removeUncertain(TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon synonymParent, Qname removedSynonymTaxonId) throws Exception {
		Qname subject = removedSynonymTaxonId;
		Predicate predicate = ApiAddSynonymServlet.UNCERTAIN_CIRCUMSCRIPTION;
		Qname object = synonymParent.getTaxonConceptQname();
		deleteStatement(dao, subject, predicate, object);

		EditableTaxon removedTaxon = (EditableTaxon) taxonomyDAO.getTaxon(removedSynonymTaxonId);
		removedTaxon.invalidate();
	}
	
	private void removeBasionym(TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon synonymParent, Qname removedSynonymTaxonId) throws Exception {
		Qname subject = removedSynonymTaxonId;
		Predicate predicate = ApiAddSynonymServlet.BASIONYM_CIRCUMSCRIPTION;
		Qname object = synonymParent.getTaxonConceptQname();
		deleteStatement(dao, subject, predicate, object);

		EditableTaxon removedTaxon = (EditableTaxon) taxonomyDAO.getTaxon(removedSynonymTaxonId);
		removedTaxon.invalidate();
	}
	
	private void removeMisspelled(TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon synonymParent, Qname removedSynonymTaxonId) throws Exception {
		Qname subject = removedSynonymTaxonId;
		Predicate predicate = ApiAddSynonymServlet.MISSPELLED_CIRCUMSCRIPTION;
		Qname object = synonymParent.getTaxonConceptQname();
		deleteStatement(dao, subject, predicate, object);

		EditableTaxon removedTaxon = (EditableTaxon) taxonomyDAO.getTaxon(removedSynonymTaxonId);
		removedTaxon.invalidate();
	}
	
	private void removeIncludedIn(TriplestoreDAO dao, EditableTaxon synonymParent, Qname removedConceptId) throws Exception {
		Qname subject = synonymParent.getTaxonConceptQname();
		Predicate predicate = ApiAddSynonymServlet.INCLUDED_IN;
		Qname object = removedConceptId;
		deleteStatement(dao, subject, predicate, object);
	}
	
	private void removeIncludes(TriplestoreDAO dao, EditableTaxon synonymParent, Qname removedConceptId) throws Exception {
		Qname subject = removedConceptId;
		Predicate predicate = ApiAddSynonymServlet.INCLUDED_IN;
		Qname object = synonymParent.getTaxonConceptQname();
		deleteStatement(dao, subject, predicate, object);
	}

	private void deleteStatement(TriplestoreDAO dao, Qname subject, Predicate predicate, Qname object) throws Exception {
		Integer removedStatementId = getStatementId(dao, subject, predicate, object);
		if (removedStatementId != null) {
			dao.deleteStatement(removedStatementId);
		}
	}

	private Integer getStatementId(TriplestoreDAO dao, Qname subject, Predicate predicate, Qname object) throws Exception {
		Model model = dao.get(subject);
		for (Statement s : model.getStatements(predicate.getQname())) {
			if (s.getObjectResource().getQname().equals(object.toString())) {
				return s.getId();
			}
		}
		return null;
	}

	private Qname getRemovedId(HttpServletRequest req) {
		String id = req.getParameter(REMOVED_ID_PARAMETER);
		if (id == null) throw new IllegalArgumentException("Must give " + REMOVED_ID_PARAMETER + " parameter");
 		id = id.replace("MX", "MX.").replace("MC", "MC.");
 		return new Qname(id);
	}
	
}
