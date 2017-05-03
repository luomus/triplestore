package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/addsynonym/*"})
public class ApiAddSynonymServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7393608674235660598L;
	private static final Predicate MISAPPLIED_CIRCUMSCRIPTION = new Predicate("MX.misappliedCircumscription");
	private static final Predicate UNCERTAIN_CIRCUMSCRIPTION = new Predicate("MX.uncertainCircumscription");
	private static final String MC_INCLUDED_IN = "MC.includedIn";
	private static final String SYNONYM_OF_PARAMETER = "synonymOfTaxon";
	private static final Predicate CIRCUMSCRIPTION = new Predicate("MX.circumscription");
	private static final String SYNONYM_TAXON_ID_PARAMETER = "synonymTaxonId";
	private static final String SYNONYM_TYPE_PARAMETER = "synonymType";

	private static enum SynonymType { SYNONYM, MISAPPLIED, INCLUDES, INCLUDED_IN, UNCERTAIN };

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		SynonymType synonymType = getSynonymType(req);

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		EditableTaxon synonymParent = getSynonymParent(req, dao, taxonomyDAO);
		try {
			checkPermissionsToAlterTaxon(synonymParent, req);
		} catch (IllegalAccessException noAccess) {
			return new ResponseData().setData("error", noAccess.getMessage()).setViewName("api-error");

		}
		
		Collection<EditableTaxon> synonyms = getSynonyms(req, dao, taxonomyDAO);
		if (synonyms.isEmpty()) {
			return new ResponseData().setData("error", "Must give at least one new taxon or one existing taxon").setViewName("api-error");
		}
		
		// TODO remove
		Utils.debug(synonymParent.getQname(), synonymParent.getScientificName(), synonymParent.getTaxonConceptQname());
		for (EditableTaxon t : synonyms) {
			Utils.debug(t.getQname(), t.getScientificName(), t.getTaxonConceptQname());
		}
		
		Qname synonymParentConceptId = synonymParent.getTaxonConceptQname();

		if (synonymType == SynonymType.SYNONYM) {
			synonym(dao, synonyms, synonymParentConceptId);
		} else if (synonymType == SynonymType.MISAPPLIED) {
			misapplied(dao, synonyms, synonymParentConceptId);
		} else if (synonymType == SynonymType.UNCERTAIN) {
			uncertain(dao, synonyms, synonymParentConceptId);
		} else if (synonymType == SynonymType.INCLUDES) {
			includes(dao, synonyms, synonymParentConceptId);
		} else if (synonymType == SynonymType.INCLUDED_IN) {
			includedIn(dao, synonyms, synonymParentConceptId);
		} else {
			throw new UnsupportedOperationException("Unknown synonym type: "  + synonymType);
		}

		synonymParent.invalidate();

		taxonomyDAO.clearTaxonConceptLinkings();	
		synonymParent.invalidate();

		return apiSuccessResponse(res);
	}

	private void includedIn(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId) throws Exception {
		for (EditableTaxon synonym : synonyms) {
			storeIncludedIn(dao, synonymParentConceptId, synonym.getTaxonConceptQname());
		}
	}

	private void includes(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId) throws Exception {
		for (EditableTaxon synonym : synonyms) {
			storeIncludedIn(dao, synonym.getTaxonConceptQname(), synonymParentConceptId);
		}
	}

	private void storeIncludedIn(TriplestoreDAO dao, Qname subject, Qname object) throws Exception {
		Model model = dao.get(subject);
		model.addStatement(new Statement(new Predicate(MC_INCLUDED_IN), new ObjectResource(object)));
		dao.store(model);
	}

	private void uncertain(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId) throws Exception {
		storeCircumscriptionType(dao, synonyms, synonymParentConceptId, UNCERTAIN_CIRCUMSCRIPTION);
	}

	private void misapplied(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId) throws Exception {
		storeCircumscriptionType(dao, synonyms, synonymParentConceptId, MISAPPLIED_CIRCUMSCRIPTION);
	}

	private void storeCircumscriptionType(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId, Predicate type) throws Exception {
		for (EditableTaxon synonym : synonyms) {
			Model model = dao.get(synonym.getQname());
			model.addStatement(new Statement(type, new ObjectResource(synonymParentConceptId)));
			dao.store(model);
		}
	}

	private void synonym(TriplestoreDAO dao, Collection<EditableTaxon> synonyms, Qname synonymParentConceptId) throws Exception {
		Model synonymParentConceptModel = dao.get(synonymParentConceptId);
		for (EditableTaxon synonym : synonyms) {
			synonym.invalidate();
			Qname synonymConceptId = synonym.getTaxonConceptQname();
			updateReferencesToSynonymConceptToNewSynonymConcept(dao, synonymConceptId, synonymParentConceptId);
			changeIncludedInStatementsToNewSynonymConcept(dao, synonymParentConceptModel, synonymConceptId);
		}
		dao.store(synonymParentConceptModel);
	}

	private void updateReferencesToSynonymConceptToNewSynonymConcept(TriplestoreDAO dao, Qname synonymConceptId, Qname synonymParentConceptId) throws Exception {
		Collection<Model> models = dao.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).objectresource(synonymConceptId));
		for (Model model : models) {
			List<Integer> deleteStatementIds = new ArrayList<>();
			List<Statement> addedStatements = new ArrayList<>();
			for (Statement statement : model.getStatements()) {
				if (objectIs(synonymConceptId, statement)) {
					deleteStatementIds.add(statement.getId());
					addedStatements.add(new Statement(statement.getPredicate(), new ObjectResource(synonymParentConceptId)));
				}
			}
			for (int id : deleteStatementIds) {
				model.removeStatement(id);
			}
			for (Statement statement : addedStatements) {
				model.addStatement(statement);
			}
			dao.store(model);
		}
	}

	private void changeIncludedInStatementsToNewSynonymConcept(TriplestoreDAO dao, Model synonymParentConceptModel, Qname synonymConceptId) throws Exception {
		Model model = dao.get(synonymConceptId);
		for (Statement statement : model.getStatements(MC_INCLUDED_IN)) {
			dao.deleteStatement(statement.getId());
			synonymParentConceptModel.addStatement(statement);
		}
	}

	private boolean objectIs(Qname taxonConceptQname, Statement statement) {
		return statement.isResourceStatement() && statement.getObjectResource().getQname().equals(taxonConceptQname.toString());
	}

	private EditableTaxon getSynonymParent(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		Qname synonymParentQname = new Qname(req.getParameter(SYNONYM_OF_PARAMETER).replace("MX", "MX."));
		EditableTaxon synonymParent = (EditableTaxon) taxonomyDAO.getTaxon(synonymParentQname);

		if (!given(synonymParent.getTaxonConceptQname())) { // shouldn't be any
			Qname taxonConceptId = dao.addTaxonConcept();
			dao.store(new Subject(synonymParentQname), new Statement(CIRCUMSCRIPTION, new ObjectResource(taxonConceptId)));
			synonymParent.setTaxonConceptQname(taxonConceptId);
		}

		return synonymParent;
	}

	private Collection<EditableTaxon> getSynonyms(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		Collection<EditableTaxon> newTaxons = parseAndCreateNewTaxons(req, dao, taxonomyDAO);
		Collection<EditableTaxon> existingTaxons = getExistingTaxons(req, taxonomyDAO);
		Collection<EditableTaxon> synonyms = new ArrayList<>();
		synonyms.addAll(newTaxons);
		synonyms.addAll(existingTaxons);
		return synonyms;
	}

	private Collection<EditableTaxon> getExistingTaxons(HttpServletRequest req, TaxonomyDAO dao) throws Exception {
		Collection<Qname> ids = getSynonymTaxonIds(req);
		if (ids.isEmpty()) return Collections.emptyList();
		List<EditableTaxon> taxons = new ArrayList<>();
		for (Qname id : ids) {
			taxons.add((EditableTaxon) dao.getTaxon(id));
		}
		return taxons;
	}

	private Collection<Qname> getSynonymTaxonIds(HttpServletRequest req) {
		if (req.getParameter(SYNONYM_TAXON_ID_PARAMETER) == null) return Collections.emptyList();
		Set<Qname> qnames = new HashSet<>();
		for (String param : req.getParameterValues(SYNONYM_TAXON_ID_PARAMETER)) {
			Qname qname = new Qname(param);
			if (qname.isSet()) {
				qnames.add(qname);
			}
		}
		return qnames;
	}

	private SynonymType getSynonymType(HttpServletRequest req) {
		String type = req.getParameter(SYNONYM_TYPE_PARAMETER);
		if (type == null) throw new IllegalArgumentException();
		return SynonymType.valueOf(type);
	}

}
