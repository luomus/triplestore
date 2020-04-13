package fi.luomus.triplestore.taxonomy.service;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.models.UsedAndGivenStatements.Used;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.service.EvaluationEditServlet;
import fi.luomus.triplestore.taxonomy.iucn.service.EvaluationEditServlet.Habitats;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;
import fi.luomus.triplestore.taxonomy.service.ApiAddSynonymServlet.SynonymType;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonEditSectionSubmit/*"})
public class ApiTaxonEditSectionSubmitServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -5176480667635744000L;
	private static final Predicate OCCURRENCE_IN_FINLAND_PUBLICATION_PREDICATE = new Predicate("MX.occurrenceInFinlandPublication");
	private static final Predicate ORIGINAL_PUBLICATION_PREDICATE = new Predicate("MX.originalPublication");
	private static final String MX_TAXON = "MX.taxon";
	private static final String VALIDATION_RESULTS = "validationResults";
	private static final String MX_SCIENTIFIC_NAME_AUTHORSHIP = "MX.scientificNameAuthorship";
	private static final Predicate AUTHOR_PREDICATE = new Predicate(MX_SCIENTIFIC_NAME_AUTHORSHIP);
	private static final String MX_SCIENTIFIC_NAME = "MX.scientificName";
	private static final Predicate SCIENTIFICNAME_PREDICATE = new Predicate(MX_SCIENTIFIC_NAME);
	private static final String MO_OCCURRENCE = "MO.occurrence";
	private static final String CONTEXT = "_CONTEXT_";
	private static final String EN = "en";
	private static final String SV = "sv";
	private static final String FI = "fi";
	private static final String RU = "ru";
	private static final String SE = "se";
	public static final Set<String> SUPPORTED_LOCALES = Utils.set(FI, SV, EN, RU, SE);

	@Override
	protected Set<User.Role> allowedRoles() {
		return TaxonDescriptionsServlet.ALLOWED;
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = new ResponseData().setViewName("api-taxoneditsubmit");
		Qname taxonQname = new Qname(req.getParameter("taxonQname"));
		String newPublicationCitation = req.getParameter("newPublicationCitation");
		String newOccurrenceInFinlandPublicationCitation = req.getParameter("newOccurrenceInFinlandPublicationCitation");
		String alteredScientificName = req.getParameter("alteredScientificName");
		String alteredAuthor = req.getParameter("alteredAuthor");
		boolean storeBiogeographicalProvinceOccurrences = storeBiogeographicalProvinceOccurrences(req); 
		boolean storeHabitats = storeHabitats(req);
		String savedLocale = savedLocale(req);

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		RdfProperties properties = dao.getProperties(MX_TAXON);
		UsedAndGivenStatements usedAndGivenStatements = parseUsedAndGivenStatements(req, properties, savedLocale);

		boolean editingDescriptionFields = editingDescriptionFields(usedAndGivenStatements, dao); 
		if (!editingDescriptionFields) {
			checkPermissionsToAlterTaxon(taxonQname, req);
		}

		if (!editingDescriptionFields) {
			if (given(newPublicationCitation)) {
				Publication publication = storePublication(newPublicationCitation, dao);
				usedAndGivenStatements.addStatement(new Statement(ORIGINAL_PUBLICATION_PREDICATE, new ObjectResource(publication.getQname())));
				responseData.setData("addedPublication", publication);
			}
			if (given(newOccurrenceInFinlandPublicationCitation)) {
				Publication publication = storePublication(newOccurrenceInFinlandPublicationCitation, dao);
				usedAndGivenStatements.addStatement(new Statement(OCCURRENCE_IN_FINLAND_PUBLICATION_PREDICATE, new ObjectResource(publication.getQname())));
				responseData.setData("addedOccurrenceInFinlandPublication", publication);
			}	
		}

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);

		if (!editingDescriptionFields) {
			if (given(alteredScientificName)) {
				setNewScientificNameAndAuthor(alteredScientificName, alteredAuthor, usedAndGivenStatements);
				createAndStoreSynonym(dao, taxonomyDAO, taxon);
			}
		}

		if (storeBiogeographicalProvinceOccurrences) {
			storeOccurrences(req, dao, taxon, taxonomyDAO);
		} else if (storeHabitats) {
			storeHabitats(req, dao, taxon);
		} else {
			dao.store(new Subject(taxonQname), usedAndGivenStatements);
		}

		taxon.invalidateSelf();

		ValidationData validationData;
		if (editingDescriptionFields) {
			validationData = new TaxonValidator(dao, taxonomyDAO, getErrorReporter()).validateDescriptions(usedAndGivenStatements.getGivenStatements());
		} else {
			taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);
			validationData = new TaxonValidator(dao, taxonomyDAO, getErrorReporter()).validate(taxon);	
		}

		return responseData.setData(VALIDATION_RESULTS, validationData);
	}

	private String savedLocale(HttpServletRequest req) {
		String classes = req.getParameter("classes");
		if (!given(classes)) return null;
		for (String locale : SUPPORTED_LOCALES) {
			if (classes.contains("locale___"+locale)) return locale;
		}
		return null;
	}

	private boolean storeBiogeographicalProvinceOccurrences(HttpServletRequest req) {
		String classes = req.getParameter("classes");
		if (!given(classes)) return false;
		return classes.contains("biogeographicalProvinceOccurrences");
	}

	private boolean storeHabitats(HttpServletRequest req) {
		String classes = req.getParameter("classes");
		if (!given(classes)) return false;
		return classes.contains("habitats");
	}

	private static Set<Qname> supportedAreas = null;

	private Set<Qname> getSupportedAreas(ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		if (supportedAreas == null) {
			Set<Qname> areas = new HashSet<>();
			for (String area : taxonomyDAO.getBiogeographicalProvinces().keySet()) {
				areas.add(new Qname(area));
			}
			supportedAreas = areas;
		}
		return supportedAreas;
	}

	public static void createAndStoreSynonym(TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, EditableTaxon taxon) throws Exception {
		EditableTaxon synonym = taxonomyDAO.createTaxon();
		synonym.setScientificName(taxon.getScientificName());
		synonym.setScientificNameAuthorship(taxon.getScientificNameAuthorship());
		if (given(taxon.getTaxonRank())) {
			synonym.setTaxonRank(taxon.getTaxonRank());
		}
		dao.addTaxon(synonym);
		dao.insert(new Subject(taxon.getQname()), new Statement(ApiAddSynonymServlet.getPredicate(SynonymType.SYNONYM), new ObjectResource(synonym.getQname())));
	}

	private void setNewScientificNameAndAuthor(String alteredScientificName, String alteredAuthor, UsedAndGivenStatements usedAndGivenStatements) {
		Iterator<Statement> i = usedAndGivenStatements.getGivenStatements().iterator();
		while (i.hasNext()) {
			Statement s = i.next();
			if (s.getPredicate().getQname().equals(MX_SCIENTIFIC_NAME)) {
				i.remove();
			}
			if (s.getPredicate().getQname().equals(MX_SCIENTIFIC_NAME_AUTHORSHIP)) {
				i.remove();
			}
		}
		usedAndGivenStatements.addStatement(new Statement(SCIENTIFICNAME_PREDICATE, new ObjectLiteral(alteredScientificName)));
		usedAndGivenStatements.addStatement(new Statement(AUTHOR_PREDICATE, new ObjectLiteral(alteredAuthor)));
	}

	private boolean editingDescriptionFields(UsedAndGivenStatements usedAndGivenStatements, TriplestoreDAO dao) {
		if (usedAndGivenStatements.getUsed().isEmpty()) return false;
		Set<String> descriptionFields = getDescriptionFields(dao);
		for (Used used : usedAndGivenStatements.getUsed()) {
			if (!descriptionFields.contains(used.getPredicate().getQname())) return false;

		}
		return true;
	}

	private Set<String> getDescriptionFields(TriplestoreDAO dao) {
		Set<String> fieldQnames = new HashSet<>();
		Map<String, List<RdfProperty>> descriptionVariables = dao.getDescriptionGroupVariables();
		for (List<RdfProperty> properties : descriptionVariables.values()) {
			for (RdfProperty p : properties) {
				fieldQnames.add(p.getQname().toString());
			}
		}
		return fieldQnames;
	}

	private void storeHabitats(HttpServletRequest req, TriplestoreDAO dao, EditableTaxon taxon) throws Exception {
		Habitats habitats = EvaluationEditServlet.parseHabitats(req);
		UsedAndGivenStatements statements = new UsedAndGivenStatements();
		statements.addUsed(IucnDAO.PRIMARY_HABITAT_PREDICATE, null, null);
		statements.addUsed(IucnDAO.SECONDARY_HABITAT_PREDICATE, null, null);
		if (habitats.primaryHabitat != null && given(habitats.primaryHabitat.getHabitat())) {
			dao.store(habitats.primaryHabitat);
			statements.addStatement(new Statement(IucnDAO.PRIMARY_HABITAT_PREDICATE, new ObjectResource(habitats.primaryHabitat.getId())));
			for (HabitatObject h : habitats.secondaryHabitats) {
				if (!given(h.getHabitat())) continue;
				dao.store(h);
				statements.addStatement(new Statement(IucnDAO.SECONDARY_HABITAT_PREDICATE, new ObjectResource(h.getId())));
			}
		}
		dao.store(new Subject(taxon.getQname()), statements);
	}

	private void storeOccurrences(HttpServletRequest req, TriplestoreDAO dao, EditableTaxon taxon, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		Set<Qname> supportedAreas = getSupportedAreas(taxonomyDAO);
		Occurrences occurrences = new Occurrences(taxon.getQname());
		for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (!parameterName.startsWith(MO_OCCURRENCE)) continue;
			if (e.getValue() == null || e.getValue().length < 1) continue;
			String value = e.getValue()[0];
			if (!given(value)) continue;
			parseOccurrence(parameterName, value, occurrences);
		}
		taxonomyDAO.addOccurrences(taxon);
		dao.store(taxon.getOccurrences(), occurrences, supportedAreas);
	}

	private void parseOccurrence(String parameterName, String value, Occurrences occurrences) {
		// MO.occurrence___ML.xxx___status
		// MO.occurrence___ML.xxx___notes
		// MO.occurrence___ML.xxx___year
		// MO.occurrence___ML.xxx___specimenURI
		Qname areaQname = splitAreaQname(parameterName);
		String field = splitField(parameterName);
		Occurrence occurrence = occurrences.getOccurrence(areaQname);
		if (occurrence == null) {
			occurrence = new Occurrence(null, areaQname, null);
		}
		if (field.equals("status")) {
			occurrence.setStatus(new Qname(value));
		} else if (field.equals("notes")) {
			occurrence.setNotes(value);
		} else if (field.equals("year")) {
			occurrence.setYear(parseYear(value));
		} else if (field.equals("specimenURI")) {
			occurrence.setSpecimenURI(parseURI(value));
		}
		occurrences.setOccurrence(occurrence);
	}

	private URI parseURI(String value) {
		if (!given(value)) return null;
		try {
			return new URI(value);
		} catch (Exception e) {
			return null;
		}
	}

	private Integer parseYear(String value) {
		try {
			String s = "";
			for (char c : value.toCharArray()) {
				if (Character.isDigit(c)) {
					s += c;
				}
			}
			if (s.isEmpty()) return null;
			return Integer.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	private Qname splitAreaQname(String parameterName) {
		return new Qname(parameterName.split(Pattern.quote("___"))[1]);
	}

	private String splitField(String parameterName) {
		return parameterName.split(Pattern.quote("___"))[2];
	}

	private Publication storePublication(String newPublicationCitation, TriplestoreDAO dao) throws Exception {
		Publication publication = new Publication(null);
		publication.setCitation(newPublicationCitation);
		dao.storePublication(publication);
		getTaxonomyDAO().getPublicationsForceReload();
		return publication;
	}

	private UsedAndGivenStatements parseUsedAndGivenStatements(HttpServletRequest req, RdfProperties properties, String savedLocale) {
		UsedAndGivenStatements usedAndGivenStatements = new UsedAndGivenStatements();
		for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (parameterName.startsWith(MO_OCCURRENCE)) continue;
			String[] values = e.getValue();

			String langcode = null;
			if (parameterName.contains("___")) {
				String[] parts = parameterName.split(Pattern.quote("___")); 
				parameterName = parts[0];
				langcode = parts[1];
			}

			Context context = null;
			if (parameterName.contains(CONTEXT)) {
				String[] parts = parameterName.split(Pattern.quote(CONTEXT));
				parameterName = parts[0];
				context = new Context(parts[1]);
			}

			Predicate predicate = new Predicate(parameterName);
			if (!properties.hasProperty(predicate.getQname())) continue;
			RdfProperty predicateProperty = properties.getProperty(predicate);

			if (savedLocale != null) {
				usedAndGivenStatements.addUsed(predicate, context, savedLocale);
			} else {
				// The UI should submit all information for this predicate in this context for all locales
				usedAndGivenStatements.addUsed(predicate, context, null);
				for (String locale : SUPPORTED_LOCALES) {
					usedAndGivenStatements.addUsed(predicate, context, locale);
				}
			}

			for (String value : values) {
				if (!given(value)) continue;
				if (predicateProperty.isLiteralProperty()) {
					usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectLiteral(value, langcode), context));
				} else {
					usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectResource(value), context));
				}
			}
		}
		return usedAndGivenStatements;
	}

}
