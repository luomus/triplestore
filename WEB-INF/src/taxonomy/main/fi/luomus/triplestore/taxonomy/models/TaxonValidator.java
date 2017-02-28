package fi.luomus.triplestore.taxonomy.models;

import java.util.List;
import java.util.Set;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.LocalizedTexts;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.BaseValidator;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

public class TaxonValidator extends BaseValidator<Taxon> {

	private static final Qname SUBGENUS = new Qname("MX.subgenus");

	private static final Qname SPECIES = new Qname("MX.species");

	private final ExtendedTaxonomyDAO dao;

	public TaxonValidator(ExtendedTaxonomyDAO dao, ErrorReporter errorReporter) {
		super(errorReporter);
		this.dao = dao;
	}

	@Override
	public void tryValidate(Taxon taxon) throws Exception {
		if (given(taxon.getScientificNameAuthorship()) && !given(taxon.getScientificName())) {
			setError("Author", "Author should be given only if scientific name has been given.");
		}

		validateScientificName(taxon);
		validateVernacularName("Vernacular name", taxon.getVernacularName(), taxon);
		validateVernacularName("Alternative vernacular name", taxon.getAlternativeVernacularNames(), taxon);
		validateVernacularName("Obsolete vernacular name", taxon.getObsoleteVernacularNames(), taxon);
		validateVernacularName("Trade name", taxon.getTradeNames(), taxon);

		if (given(taxon.getScientificName())) {
			List<Taxon> matches = dao.taxonNameExistsInChecklistForOtherTaxon(taxon.getScientificName(), taxon.getChecklist(), taxon.getQname());
			for (Taxon match : matches) {
				setError("Scientific name", "Name already used in this checklist for taxon: " + debug(match));
			}
		}
		for (String vernacularName : taxon.getVernacularName().getAllTexts().values()) {
			List<Taxon> matches = dao.taxonNameExistsInChecklistForOtherTaxon(vernacularName, taxon.getChecklist(), taxon.getQname());
			for (Taxon match : matches) {
				setError("Vernacular name", "Name " + vernacularName + " already used in this checklist for taxon: " + debug(match));
			}
		}
		for (String alternativeVernacularName : taxon.getAlternativeVernacularNames().getAllValues()) {
			List<Taxon> matches = dao.taxonNameExistsInChecklistForOtherTaxon(alternativeVernacularName, taxon.getChecklist(), taxon.getQname());
			for (Taxon match : matches) {
				setWarning("Alternative vernacular name", "Name " + alternativeVernacularName + " already used in this checklist for taxon: " + debug(match));
			}
		}
	}

	private void validateVernacularName(String fieldName, LocalizedText localizedText, Taxon taxon) {
		for (String name : localizedText.getAllTexts().values()) {
			validateVernacularName(fieldName, name, taxon);
		}
	}
	
	private void validateVernacularName(String fieldName, LocalizedTexts names, Taxon taxon) {
		for (String name : names.getAllValues()) {
			validateVernacularName(fieldName, name, taxon);
		}
	}

	private static final Set<Character> ALPHAS = Utils.set(
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 
			'w', 'x', 'y', 'z', 'å', 'ä', 'ö', 'é', 'ü', 'æ', 'í');

	private static final Set<Character> VERNACULAR_ALLOWED = Utils.set('-', ' ');

	private void validateVernacularName(String fieldName, String name, Taxon taxon) {
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
		if (SUBGENUS.equals(taxon.getTaxonRank()) || taxon.isSpecies() || taxon.getTaxonRank() == null) {
			name = allowParentheses(name);
			name = allowSpace(name);
		}
		if (!taxon.isSpecies() && name.startsWith("\"") && name.endsWith("\"")) {
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
		}
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

}
