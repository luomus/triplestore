package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import java.util.Collection;

public interface ExtendedTaxonomyDAO extends TaxonomyDAO {

	public Collection<Taxon> getChildTaxons(Taxon taxon);

	public Collection<Taxon> getSynonymTaxons(Taxon taxon);

	public void invalidateTaxon(Qname qname);

	public void invalidateTaxon(Taxon parent);

	public void addOccurrences(EditableTaxon taxon);

}
