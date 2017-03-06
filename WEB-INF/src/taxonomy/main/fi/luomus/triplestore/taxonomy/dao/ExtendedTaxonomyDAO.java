package fi.luomus.triplestore.taxonomy.dao;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ExtendedTaxonomyDAO extends TaxonomyDAO {

	public void addOccurrences(EditableTaxon taxon);

	public IucnDAO getIucnDAO();
	
	/**
	 * Checks if given name (scientific, vernacular or other name) exists in the given checklist, ignoring the taxon qname in question.
	 * @param name
	 * @param checklist
	 * @param taxonQnameToIgnore
	 * @return List of matcing taxons
	 * @throws Exception 
	 */
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Qname checklist, Qname taxonQnameToIgnore) throws Exception;

	/**
	 * Returns new taxon with newly assiged id
	 * @return
	 * @throws Exception 
	 */
	public EditableTaxon createTaxon() throws Exception;

	public Set<String> getInformalTaxonGroupRoots();

	public TaxonSearchResponse searchInternal(TaxonSearch taxonSearch) throws Exception;
	
	public Map<String, Area> getBiogeographicalProvinces() throws Exception;
	
}
