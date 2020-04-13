package fi.luomus.triplestore.taxonomy.dao;

import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public interface ExtendedTaxonomyDAO extends TaxonomyDAO {

	public void addOccurrences(EditableTaxon taxon);

	public void addHabitats(EditableTaxon taxon);
	
	public IucnDAO getIucnDAO();

	/**
	 * Checks if given name (scientific, vernacular or other name) exists in the given checklist, ignoring the taxon in question.
	 * @param name
	 * @param taxon
	 * @return list of matches
	 * @throws Exception
	 */
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Taxon taxon) throws Exception;

	/**
	 * Returns new taxon with newly assiged id
	 * @return
	 * @throws Exception 
	 */
	public EditableTaxon createTaxon() throws Exception;

	public List<String> getInformalTaxonGroupRoots();
	public List<String> getIucnRedListInformalGroupRoots();

	public Map<String, Area> getBiogeographicalProvinces() throws Exception;

	/**
	 * Is the given taxon id used explicitly in data warehouse as a target name or taxon census target (etc) 
	 * @param taxonId
	 * @return
	 */
	public boolean isTaxonIdUsedInDataWarehouse(Qname taxonId);

}
