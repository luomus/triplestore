package fi.luomus.triplestore.taxonomy.service;

import java.util.HashSet;
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
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.models.UsedAndGivenStatements.Used;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonValidator;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonEditSectionSubmit/*"})
public class ApiTaxonEditSectionSubmitServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -5176480667635744000L;
	private static final Set<String> VERNACULAR_NAMES = Utils.set("MX.vernacularName", "MX.alternativeVernacularName", "MX.obsoleteVernacularName", "MX.tradeName");
	private static final Set<String> FI_SV = Utils.set("fi", "sv");

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname taxonQname = new Qname(req.getParameter("taxonQname"));
		String newPublicationCitation = req.getParameter("newPublicationCitation");
		String newOccurrenceInFinlandPublicationCitation = req.getParameter("newOccurrenceInFinlandPublicationCitation");

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		
		RdfProperties properties = dao.getProperties("MX.taxon");
		UsedAndGivenStatements usedAndGivenStatements = parseUsedAndGivenStatements(req, properties);

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
		boolean occurrenceDataGiven = false;
		Occurrences occurrences = new Occurrences(taxon.getQname());
		for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (!parameterName.startsWith("MO.occurrence")) continue;
			occurrenceDataGiven = true;
			Qname area = new Qname(parameterName.split(Pattern.quote("___"))[1]); 
			Qname status = new Qname(e.getValue()[0]);
			if (status.isSet()) {
				occurrences.setOccurrence(null, area, status);
			}
		}
		if (occurrenceDataGiven) {
			dao.store(taxon.getOccurrences(), occurrences);
		}
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
			if (parameterName.startsWith("MO.occurrence")) continue;
			String[] values = e.getValue();

			String langcode = null;
			if (parameterName.contains("___")) {
				String[] parts = parameterName.split(Pattern.quote("___")); 
				parameterName = parts[0];
				langcode = parts[1];
			}

			Context context = null;
			if (parameterName.contains("_CONTEXT_")) {
				String[] parts = parameterName.split(Pattern.quote("_CONTEXT_"));
				parameterName = parts[0];
				context = new Context(parts[1]);
			}

			Predicate predicate = new Predicate(parameterName);
			if (!properties.hasProperty(predicate.getQname())) continue;
			RdfProperty predicateProperty = properties.getProperty(predicate);

			// luotetaan käyttöliittymään, että saadaan tälle taksonille predikaatista JA kontekstista kaikilla kielillä (tai objektina) tämän resurssi
			usedAndGivenStatements.addUsed(predicate, context, null);
			usedAndGivenStatements.addUsed(predicate, context, "fi");
			usedAndGivenStatements.addUsed(predicate, context, "sv");
			usedAndGivenStatements.addUsed(predicate, context, "en");

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
