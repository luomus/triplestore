package fi.luomus.triplestore.taxonomy.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.LocalizedTexts;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.service.TaxonDescriptionsServlet;

public class TaxonValidator {

	private static final Qname GENUS = new Qname("MX.genus");

	private static final Qname SUBGENUS = new Qname("MX.subgenus");

	private static final Qname SPECIES = new Qname("MX.species");

	private final TriplestoreDAO triplestoreDAO;
	private final ExtendedTaxonomyDAO taxonomyDAO;
	private final ErrorReporter errorReporter;
	private final ValidationData validationData;

	public TaxonValidator(TriplestoreDAO triplestoreDAO, ExtendedTaxonomyDAO taxonomyDAO, ErrorReporter errorReporter) {
		this.triplestoreDAO = triplestoreDAO;
		this.errorReporter = errorReporter;
		this.validationData = new ValidationData();
		this.taxonomyDAO = taxonomyDAO;
	}

	public ValidationData validate(Taxon taxon) {
		try {
			tryValidate(taxon);
		} catch (Exception e) {
			setError("SYSTEM ERROR", "Could not complete validations. ICT-team has been notified. Reason: " + e.getMessage());
			errorReporter.report("Validation error", e);
		}
		return validationData;
	}

	private void setWarning(String field, String warning) {
		validationData.setWarning(field, warning);
	}

	private void setError(String field, String error) {
		validationData.setError(field, error);		
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	private void tryValidate(Taxon taxon) throws Exception {
		if (given(taxon.getScientificNameAuthorship()) && !given(taxon.getScientificName())) {
			setError("Author", "Author should be given only if scientific name has been given.");
		}

		validateScientificName(taxon);
		validateVernacularName("Vernacular name", taxon.getVernacularName(), taxon);
		validateVernacularName("Other vernacular name", taxon.getAlternativeVernacularNames(), taxon);
		validateVernacularName("Obsolete vernacular name", taxon.getObsoleteVernacularNames(), taxon);
		validateVernacularName("Trade name", taxon.getTradeNames(), taxon);

		if (given(taxon.getScientificName())) {
			List<Taxon> matches = taxonomyDAO.taxonNameExistsInChecklistForOtherTaxon(taxon.getScientificName(), taxon);
			for (Taxon match : matches) {
				setError("Scientific name", "Name already used in this checklist for taxon: " + debug(match));
			}
		}
		for (String vernacularName : taxon.getVernacularName().getAllTexts().values()) {
			List<Taxon> matches = taxonomyDAO.taxonNameExistsInChecklistForOtherTaxon(vernacularName, taxon);
			for (Taxon match : matches) {
				setError("Vernacular name", "Name " + vernacularName + " already used in this checklist for taxon: " + debug(match));
			}
		}
		for (String alternativeVernacularName : taxon.getAlternativeVernacularNames().getAllValues()) {
			List<Taxon> matches = taxonomyDAO.taxonNameExistsInChecklistForOtherTaxon(alternativeVernacularName, taxon);
			for (Taxon match : matches) {
				setWarning("Other vernacular name", "Name " + alternativeVernacularName + " already used in this checklist for taxon: " + debug(match));
			}
		}
	}

	private void validateVernacularName(String fieldName, LocalizedText localizedText, Taxon taxon) {
		for (Map.Entry<String, String> e : localizedText.getAllTexts().entrySet()) {
			String locale = e.getKey();
			String name = e.getValue();
			validateVernacularName(fieldName, name, locale, taxon);
		}
	}

	private void validateVernacularName(String fieldName, LocalizedTexts names, Taxon taxon) {
		for (Entry<String, List<String>> e : names.getAllTexts().entrySet()) {
			String locale = e.getKey();
			for (String name : e.getValue()) {
				validateVernacularName(fieldName, name, locale, taxon);
			}
		}
	}

	private static final Set<Character> ALPHAS = Utils.set(
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 
			'w', 'x', 'y', 'z', 'å', 'ä', 'ö', 'é', 'ü', 'æ', 'í', 'ë');

	private static final Set<Character> VERNACULAR_ALLOWED = Utils.set('-', ' ');

	private void validateVernacularName(String fieldName, String name, String locale, Taxon taxon) {
		if (name == null) return;
		name = name.trim().toLowerCase();
		if (name.isEmpty()) return;
		if (!ALPHAS.contains(name.charAt(0))) {
			setError(fieldName, "Name should begin with an alpha. For example write the complete name \"jättiputket -ryhmä\", not just \"-ryhmä\".");
			return;
		}
		if (name.contains(",")) {
			setError(fieldName, "Name must not contain a comma. Give multiple names separately.");
			return;
		}
		if (isSubSpecies(taxon)) {
			name = allowParentheses(name);
		}
		for (char c : name.toCharArray()) {
			if (c == '\'' && "en".equals(locale)) continue;
			if (!ALPHAS.contains(c) && !VERNACULAR_ALLOWED.contains(c)) {
				setError(fieldName, "Name must not contain the character '" + c+"'");
				return;
			}
		}

	}

	private String allowParentheses(String name) {
		name = name.replace("(", "").replace(")", ""); // allow () for subspecies
		return name;
	}

	private boolean isSubSpecies(Taxon taxon) {
		return taxon.isSpecies() && !SPECIES.equals(taxon.getTaxonRank());
	}

	private static final Set<Character> SCIENTIFIC_ALLOWED_FOR_SPECIES = Utils.set('.', '/', '-', '?', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'); // numbers are for viruses

	private void validateScientificName(Taxon taxon) {
		String name = taxon.getScientificName();
		if (name == null) return;
		name = name.trim().toLowerCase(); 
		if (name.isEmpty()) return;

		if (SUBGENUS.equals(taxon.getTaxonRank())) {
			if (!name.contains("(") && !name.contains(")") && !name.contains("subg.")) {
				setError("Scientific name", "For subgenuses, use the following form \"Bombus (Bombus)\" or \"Carex subg. Carex\"");
				return;
			}
		}
		
		if ((taxon.isCursiveName() || taxon.getTaxonRank() == null) && !GENUS.equals(taxon.getTaxonRank())) {
			name = allowParentheses(name);
			name = allowSpace(name);
		}
		if (!taxon.isCursiveName() && name.startsWith("\"") && name.endsWith("\"")) {
			name = allowQuotationMarks(name); 
		}
		for (char c : name.toCharArray()) {
			if (!ALPHAS.contains(c)) {
				if (!taxon.isSpecies() || !SCIENTIFIC_ALLOWED_FOR_SPECIES.contains(c)) {
					setError("Scientific name", "Must not contain the character '" + c+ "'");
					return;					
				}
			}
		}
		if (taxon.isSpecies() && !taxon.getScientificName().contains(" ")) {
			setError("Scientific name", "Must contain a space for species and subspecies (etc). Use full form: [Genus] [specific epithet]");
			return;
		}

		if (taxon.isSpecies()) {
			Taxon genusParentTaxon = taxon.getParentOfRank(GENUS);
			if (genusParentTaxon != null) {
				String speciesGenusEpithet = parseGenus(taxon.getScientificName());
				String parentGenusName = parseGenus(genusParentTaxon.getScientificName());
				if (!speciesGenusEpithet.equals(parentGenusName)) {
					setError("Scientific name", "Genus of a species must match the name of the parent genus (" + parentGenusName + ")");
				}
			}
		}
	}

	private String parseGenus(String scientificName) {
		if (scientificName == null) return "";
		if (!scientificName.contains(" ")) return scientificName.trim();
		return scientificName.split(Pattern.quote(" "))[0].trim();
	}

	private String allowSpace(String name) {
		return name.replace(" ", "");
	}

	private String allowQuotationMarks(String name) {
		name = name.replace("\"", "");
		return name;
	}

	private String debug(Taxon taxon) {
		StringBuilder b = new StringBuilder();
		b.append(taxon.getQname()).append(", ").append(taxon.getScientificName()).append(" ");
		if (given(taxon.getScientificNameAuthorship())) {
			b.append(taxon.getScientificNameAuthorship());
		}
		b.append(" [");
		if (given(taxon.getTaxonRank())) {
			b.append(taxon.getTaxonRank().toString().replace("MX.", ""));
		} else {
			b.append("NO RANK");
		}
		b.append("]");
		String s = b.toString();
		while (s.contains("  ")) {
			s = s.replace("  ", " ");
		}
		return s;
	}

	public ValidationData validateDescriptions(Collection<Statement> statements) {
		try {
			for (Statement s : statements) {
				validateDescription(s);
			}
		} catch (Exception e) {
			setError("SYSTEM ERROR", "Could not complete validations. ICT-team has been notified. Reason: " + e.getMessage());
			errorReporter.report("Validation error", e);
		}
		return validationData;
	}

	private void validateDescription(Statement s) {
		String content = s.getObjectLiteral().getUnsanitazedContent();
		
		if (Utils.countOfUTF8Bytes(content) >= ObjectLiteral.MAX_BYTE_LENGTH) {
			setError(getFieldDescription(s.getPredicate()), "The text was too long and it has been shortened!");
		}
		
		Set<String> tags = parseTags(content);
		tags.removeAll(ALLOWED_TAGS);
		if (!tags.isEmpty()) {
			setError(getFieldDescription(s.getPredicate()), "Unallowed tag: " + tags.iterator().next().replace("/", "") + ". They were removed from the saved content! Allowed tags are: " + ObjectLiteral.ALLOWED_TAGS);
			return ;
		}
		
		if (hasStyles(content)) {
			setWarning(getFieldDescription(s.getPredicate()), "Custom styles are discouraged");
		}
	}

	private boolean hasStyles(String content) {
		return Utils.removeWhitespace(content).contains("style=");
	}

	private String getFieldDescription(Predicate predicate) {
		Map<String, List<RdfProperty>> variables = TaxonDescriptionsServlet.cachedDescriptionGroupVariables.get(triplestoreDAO);
		for (List<RdfProperty> properties : variables.values()) {
			for (RdfProperty p : properties) {
				if (p.getQname().toString().equals(predicate.getQname())) {
					try {
						return p.getLabel().forLocale("fi") + " - " + p.getLabel().forLocale("en");
					} catch  (Exception e) {
						return p.getQname().toString();
					}
				}
			}
		}
		throw new IllegalStateException("No desc variable found: " + predicate.getQname());
	}

	private static final Collection<String> ALLOWED_TAGS; 
	static {
		ALLOWED_TAGS = new ArrayList<>();
		for (String tag : ObjectLiteral.ALLOWED_TAGS.split(Pattern.quote(","))) {
			tag = tag.trim();
			ALLOWED_TAGS.add(tag);
			ALLOWED_TAGS.add("/"+tag);
		}
	}

	private Set<String> parseTags(String content) {
		Set<String> tags = new HashSet<>();
		boolean tagOpen = false;
		String tag = "";
		for (char c : content.toCharArray()) {
			if (c == '<') {
				tagOpen = true;
				continue;
			}
			if (tagOpen && (c == '>' || (c == ' ' && !tag.isEmpty()))) {
				tagOpen = false;
				if (!tag.isEmpty()) tags.add(tag);
				tag = "";
				continue;
			}
			if (tagOpen) {
				tag += c;
				tag = tag.trim();
			}
		}
		return tags;
	}

}
