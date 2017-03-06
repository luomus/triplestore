package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
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
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.models.UsedAndGivenStatements.Used;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonEditSectionSubmit/*"})
public class ApiTaxonEditSectionSubmitServlet extends ApiBaseServlet {

	private static final String MO_OCCURRENCE = "MO.occurrence";
	private static final String CONTEXT = "_CONTEXT_";
	private static final String EN = "en";
	private static final String SV = "sv";
	private static final String FI = "fi";
	private static final String IS_PART_OF_INFORMAL_TAXON_GROUP = "MX.isPartOfInformalTaxonGroup";
	private static final long serialVersionUID = -5176480667635744000L;
	private static final Set<String> VERNACULAR_NAMES = Utils.set("MX.vernacularName", "MX.alternativeVernacularName", "MX.obsoleteVernacularName", "MX.tradeName");
	private static final Set<String> FI_SV = Utils.set(FI, SV);

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname taxonQname = new Qname(req.getParameter("taxonQname"));
		String newPublicationCitation = req.getParameter("newPublicationCitation");
		String newOccurrenceInFinlandPublicationCitation = req.getParameter("newOccurrenceInFinlandPublicationCitation");

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		RdfProperties properties = dao.getProperties("MX.taxon");
		UsedAndGivenStatements usedAndGivenStatements = parseUsedAndGivenStatements(req, properties);
		addParentInformalGroupsIfGiven(usedAndGivenStatements, taxonomyDAO);

		boolean editingDescriptionFields = !containsNonDescriptionFields(usedAndGivenStatements, dao); 
		if (!editingDescriptionFields) {
			checkPermissionsToAlterTaxon(taxonQname, req);
		}

		if (given(newPublicationCitation)) {
			Publication publication = storePublication(newPublicationCitation, dao);
			usedAndGivenStatements.addStatement(new Statement(new Predicate("MX.originalPublication"), new ObjectResource(publication.getQname())));
		}
		if (given(newOccurrenceInFinlandPublicationCitation)) {
			Publication publication = storePublication(newOccurrenceInFinlandPublicationCitation, dao);
			usedAndGivenStatements.addStatement(new Statement(new Predicate("MX.occurrenceInFinlandPublication"), new ObjectResource(publication.getQname())));
		}

		dao.store(new Subject(taxonQname), usedAndGivenStatements);

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);
		storeOccurrences(req, dao, taxon);

		taxon.invalidate();
		taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);
		ValidationData validationData;
		if (editingDescriptionFields) {
			validationData = new TaxonValidator(dao, taxonomyDAO, getErrorReporter()).validateDescriptions(usedAndGivenStatements.getGivenStatements());
		} else {
			validationData = new TaxonValidator(dao, taxonomyDAO, getErrorReporter()).validate(taxon);	
		}

		return new ResponseData().setViewName("api-taxoneditsubmit").setData("validationResults", validationData);
	}

	private void addParentInformalGroupsIfGiven(UsedAndGivenStatements usedAndGivenStatements, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		List<Statement> newStatements = new ArrayList<>();
		for (Statement s : usedAndGivenStatements.getGivenStatements()) {
			if (s.getPredicate().toString().equals(IS_PART_OF_INFORMAL_TAXON_GROUP)) {
				newStatements.addAll(parentInformalGroups(s, taxonomyDAO));
			}
		}
		for (Statement s : newStatements) {
			usedAndGivenStatements.addStatement(s);
		}
	}

	private Collection<Statement> parentInformalGroups(Statement s, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		if (!s.isResourceStatement()) return Collections.emptyList();
		if (!given(s.getObjectResource().getQname())) return Collections.emptyList();

		List<Statement> parentStatements = new ArrayList<>();
		String informalGroupId = s.getObjectResource().getQname();
		InformalTaxonGroup group = taxonomyDAO.getInformalTaxonGroups().get(informalGroupId);
		if (group == null) return Collections.emptyList();

		while (group.hasParent()) {
			parentStatements.add(new Statement(new Predicate(IS_PART_OF_INFORMAL_TAXON_GROUP), new ObjectResource(group.getParent())));
			group = taxonomyDAO.getInformalTaxonGroups().get(group.getParent().toString());
			if (group == null) break;
		}
		return parentStatements;
	}

	private boolean containsNonDescriptionFields(UsedAndGivenStatements usedAndGivenStatements, TriplestoreDAO dao) {
		Set<String> descriptionFields = getDescriptionFields(dao);
		for (Used used : usedAndGivenStatements.getUsed()) {
			if (!descriptionFields.contains(used.getPredicate().getQname())) return true;

		}
		return false;
	}

	private Set<String> getDescriptionFields(TriplestoreDAO dao) {
		Set<String> fieldQnames = new HashSet<>();
		Map<String, List<RdfProperty>> descriptionVariables = TaxonDescriptionsServlet.cachedDescriptionGroupVariables.get(dao);
		for (List<RdfProperty> properties : descriptionVariables.values()) {
			for (RdfProperty p : properties) {
				fieldQnames.add(p.getQname().toString());
			}
		}
		return fieldQnames;
	}

	private void storeOccurrences(HttpServletRequest req, TriplestoreDAO dao, Taxon taxon) throws Exception {
		Occurrences occurrences = new Occurrences(taxon.getQname());
		for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (!parameterName.startsWith(MO_OCCURRENCE)) continue;
			if (e.getValue() == null || e.getValue().length < 1) continue;
			String value = e.getValue()[0];
			if (!given(value)) continue;
			parseOccurrence(parameterName, value, occurrences);
		}
		if (occurrences.hasOccurrences()) {
			dao.store(taxon.getOccurrences(), occurrences);
		}
	}

	private void parseOccurrence(String parameterName, String value, Occurrences occurrences) {
		// MO.occurrence___ML.xxx___status
		// MO.occurrence___ML.xxx___notes
		// MO.occurrence___ML.xxx___year
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
		}
		occurrences.setOccurrence(occurrence);
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
		Publication publication = new Publication(dao.getSeqNextValAndAddResource("MP"));
		publication.setCitation(newPublicationCitation);
		dao.storePublication(publication);
		getTaxonomyDAO().getPublicationsForceReload();
		return publication;
	}

	private UsedAndGivenStatements parseUsedAndGivenStatements(HttpServletRequest req, RdfProperties properties) {
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

			// The UI should submit all information for this predicate in this context for all languages
			usedAndGivenStatements.addUsed(predicate, context, null);
			usedAndGivenStatements.addUsed(predicate, context, FI);
			usedAndGivenStatements.addUsed(predicate, context, SV);
			usedAndGivenStatements.addUsed(predicate, context, EN);

			for (String value : values) {
				if (!given(value)) continue;
				if (predicateProperty.isLiteralProperty()) {
					value = cleanPossibleVernacularName(parameterName, langcode, value);
					usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectLiteral(value, langcode), context));
				} else {
					usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectResource(value), context));
				}
			}
		}
		return usedAndGivenStatements;
	}

	private String cleanPossibleVernacularName(String parameterName, String langcode, String value) {
		if (VERNACULAR_NAMES.contains(parameterName) && FI_SV.contains(langcode)) {
			value = value.toLowerCase();
		}
		return value;
	}

}
