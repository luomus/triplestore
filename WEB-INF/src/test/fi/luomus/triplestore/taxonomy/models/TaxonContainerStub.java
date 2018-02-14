package fi.luomus.triplestore.taxonomy.models;

import java.util.Set;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Filter;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;

public class TaxonContainerStub implements TaxonContainer {

	@Override
	public Filter getAdminStatusFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<Qname> getChildren(Qname arg0) {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Filter getInformalGroupFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<Qname> getInvasiveSpeciesFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Filter getRedListStatusFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Taxon getTaxon(Qname arg0) throws NoSuchTaxonException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Filter getTypesOfOccurrenceFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTaxon(Qname arg0) {
		// Auto-generated method stub
		return false;
	}

	@Override
	public int getNumberOfTaxons() throws UnsupportedOperationException {
		// Auto-generated method stub
		return 0;
	}

	@Override
	public Set<Qname> getHasDescriptionsFilter() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<Qname> getHasMediaFilter() throws UnsupportedOperationException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Set<Qname> getInvasiveSpeciesEarlyWarningFilter() {
		// TAuto-generated method stub
		return null;
	}

}
