package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

@WebServlet(urlPatterns = {"/taxonomy-editor/api/addSynonym/*"})
public class ApiAddSynonymServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7393608674235660598L;

	public static final Predicate HAS_BASIONYM = new Predicate("MX.hasBasionym");
	public static final Predicate HAS_MISAPPLIED = new Predicate("MX.hasMisappliedName");
	public static final Predicate HAS_UNCERTAIN = new Predicate("MX.hasUncertainSynonym");
	public static final Predicate HAS_MISSPELLED = new Predicate("MX.hasMisspelledName");
	public static final Predicate HAS_SYNONYM = new Predicate("MX.hasSynonym");
	public static final Predicate HAS_OBJECTIVE = new Predicate("MX.hasObjectiveSynonym");
	public static final Predicate HAS_SUBJECTIVE = new Predicate("MX.hasSubjectiveSynonym");
	public static final Predicate HAS_HOMOTYPIC  = new Predicate("MX.hasHomotypicSynonym");
	public static final Predicate HAS_HETEROTYPIC  = new Predicate("MX.hasHeterotypicSynonym");
	public static final Predicate HAS_ORTOGRAPHIC  = new Predicate("MX.hasOrthographicVariant");
	public static final Map<SynonymType, Predicate> SYNONYM_PREDICATES;
	static {
		SYNONYM_PREDICATES = new HashMap<>();
		SYNONYM_PREDICATES.put(SynonymType.SYNONYM, HAS_SYNONYM);
		SYNONYM_PREDICATES.put(SynonymType.MISAPPLIED, HAS_MISAPPLIED);
		SYNONYM_PREDICATES.put(SynonymType.UNCERTAIN, HAS_UNCERTAIN);
		SYNONYM_PREDICATES.put(SynonymType.MISSPELLED, HAS_MISSPELLED);
		SYNONYM_PREDICATES.put(SynonymType.BASIONYM, HAS_BASIONYM);
		SYNONYM_PREDICATES.put(SynonymType.OBJECTIVE, HAS_OBJECTIVE);
		SYNONYM_PREDICATES.put(SynonymType.SUBJECTIVE, HAS_SUBJECTIVE);
		SYNONYM_PREDICATES.put(SynonymType.HOMOTYPIC, HAS_HOMOTYPIC);
		SYNONYM_PREDICATES.put(SynonymType.HETEROTYPIC, HAS_HETEROTYPIC);
		SYNONYM_PREDICATES.put(SynonymType.ORTOGRAPHIC, HAS_ORTOGRAPHIC);
	}
	public static final String SYNONYM_OF_PARAMETER = "synonymOfTaxon";
	private static final String SYNONYM_TYPE_PARAMETER = "synonymType";

	public static enum SynonymType { SYNONYM, MISAPPLIED, UNCERTAIN, MISSPELLED, BASIONYM, OBJECTIVE, SUBJECTIVE, HOMOTYPIC, HETEROTYPIC, ORTOGRAPHIC };

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

		Collection<EditableTaxon> synonyms = parseAndCreateNewTaxons(req, dao, taxonomyDAO);
		if (synonyms.isEmpty()) {
			return new ResponseData().setData("error", "Must give at least one new taxon or one existing taxon").setViewName("api-error");
		}

		for (EditableTaxon synonym : synonyms) {
			dao.insert(new Subject(synonymParent.getQname()), new Statement(getPredicate(synonymType), new ObjectResource(synonym.getQname())));
		}

		synonymParent.invalidate();

		return apiSuccessResponse(res);
	}

	public static EditableTaxon getSynonymParent(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		Qname synonymParentQname = new Qname(req.getParameter(SYNONYM_OF_PARAMETER).replace("MX", "MX."));
		EditableTaxon synonymParent = (EditableTaxon) taxonomyDAO.getTaxon(synonymParentQname);
		return synonymParent;
	}

	private Collection<EditableTaxon> parseAndCreateNewTaxons(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		List<EditableTaxon> taxons = new ArrayList<>();
		Map<Integer, Map<String, String>> newTaxonValues = parseNewTaxonValues(req);
		for (Map<String, String> taxonData : newTaxonValues.values()) {
			String sciName = taxonData.get("scientificName");
			if (!given(sciName)) continue;
			EditableTaxon taxon = createTaxon(sciName, taxonomyDAO);
			taxon.setScientificNameAuthorship(taxonData.get("authors"));
			if (taxonData.containsKey("rank")) {
				taxon.setTaxonRank(new Qname(taxonData.get("rank")));
			}
			dao.addTaxon(taxon);
			taxons.add(taxon);
		}
		return taxons;
	}

	private Map<Integer, Map<String, String>> parseNewTaxonValues(HttpServletRequest req) {
		Map<Integer, Map<String, String>> map = new HashMap<>();
		Enumeration<String> e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String parameter = e.nextElement();
			if (!parameter.contains("___")) continue;
			String value = req.getParameter(parameter);
			if (!given(value)) continue;
			String[] parts = parameter.split(Pattern.quote("___"));
			int index = Integer.valueOf(parts[1]);
			String field = parts[0];
			if (!map.containsKey(index)) map.put(index, new HashMap<String, String>());
			map.get(index).put(field, value);
		}
		return map;
	}

	public static SynonymType getSynonymType(HttpServletRequest req) {
		String type = req.getParameter(SYNONYM_TYPE_PARAMETER);
		if (type == null) throw new IllegalArgumentException();
		return SynonymType.valueOf(type);
	}

	public static Predicate getPredicate(SynonymType synonymType) {
		Predicate predicate = SYNONYM_PREDICATES.get(synonymType);
		if (predicate == null) throw new UnsupportedOperationException(synonymType.toString());
		return predicate;
	}


}
