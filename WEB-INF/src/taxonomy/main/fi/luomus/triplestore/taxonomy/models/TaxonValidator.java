package fi.luomus.triplestore.taxonomy.models;

import java.util.List;

import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.models.BaseValidator;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

public class TaxonValidator extends BaseValidator<Taxon> {

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

	private String debug(Taxon taxon) {
		StringBuilder b = new StringBuilder();
		b.append(taxon.getQname()).append(" ").append(taxon.getScientificName()).append(" ");
		if (given(taxon.getScientificNameAuthorship())) {
			b.append(", ").append(taxon.getScientificNameAuthorship());
		}
		b.append(" [");
		if (given(taxon.getTaxonRank())) {
			b.append(taxon.getTaxonRank().toString().replace("MX.", ""));
		} else {
			b.append("NO RANK");
		}
		b.append("]");
		return b.toString();
	}

}
