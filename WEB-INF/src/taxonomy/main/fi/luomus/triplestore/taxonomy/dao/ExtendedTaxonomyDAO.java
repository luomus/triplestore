package fi.luomus.triplestore.taxonomy.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

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
	
	public Map<String, Area> getBiogeographicalProvinces() throws Exception;

	public void clearTaxonConceptLinkings();
	
}
