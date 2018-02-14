package fi.luomus.triplestore.taxonomy.models;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.AdministrativeStatus;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.ContentContextDescription;
import fi.luomus.commons.containers.ContentGroups;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.Person;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;

public class TaxonomyDAOStub implements ExtendedTaxonomyDAO {

	@Override
	public void clearCaches() {
		// Auto-generated method stub
		
	}

	@Override
	public Map<String, AdministrativeStatus> getAdministrativeStatuses() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, AdministrativeStatus> getAdministrativeStatusesForceReload() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Area> getAreas() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Checklist> getChecklists() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Checklist> getChecklistsForceReload() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ContentContextDescription> getContentContextDescriptions() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public ContentGroups getContentGroups() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, InformalTaxonGroup> getInformalTaxonGroups() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, InformalTaxonGroup> getInformalTaxonGroupsForceReload() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public LocalizedText getLabels(Qname arg0) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Person> getPersons() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Publication> getPublications() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Publication> getPublicationsForceReload() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Taxon getTaxon(Qname arg0) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public TaxonContainer getTaxonContainer() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Qname> getTaxonRanks() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void addOccurrences(EditableTaxon taxon) {
		// Auto-generated method stub
		
	}

	@Override
	public IucnDAO getIucnDAO() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Qname checklist, Qname taxonQnameToIgnore) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public EditableTaxon createTaxon() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getInformalTaxonGroupRoots() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Area> getBiogeographicalProvinces() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public TaxonSearchResponse search(TaxonSearch arg0) throws Exception {
		// Auto-generated method stub
		return null;
	}

}
