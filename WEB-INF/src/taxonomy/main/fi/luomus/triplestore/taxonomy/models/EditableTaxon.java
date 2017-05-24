package fi.luomus.triplestore.taxonomy.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;

public class EditableTaxon extends Taxon {

	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");
	private static final Qname MASTER_CHECKLIST = new Qname("MR.1");


	private final CachedLiveLoadingTaxonContainer taxonContainer;

	public EditableTaxon(Qname qname, CachedLiveLoadingTaxonContainer taxonContainer) {
		super(qname, taxonContainer);
		this.taxonContainer = taxonContainer;
	}

	@Override
	public int getCountOfSpecies() {
		throw new UnsupportedOperationException();
	}

	public boolean allowsAlterationsBy(User user) {
//		if (user.isAdmin()) {
//			return true;
//		}

		if (allowsAlterationForUserDirectly(user, this)) {
			return true;
		}

		// Allow editor to edit 'one step lower' than he has permissions
		if (this.hasParent()) {
			if (allowsAlterationForUserDirectly(user, this.getParent().getEditors())) {
				return true;
			}
		}

		// We start checking if user would have permissions to alter this taxon because this taxon is a synonym of something, that the editor has permissions to edit

		// First some preconditions
		if (this.getChecklist() != null) {
			return false; // This is a taxon in some checklist: editor has to have permissions to edit this taxon in that checklist  
		}

		Collection<Taxon> allSynonymTypes = getAllSynonyms(this);

		for (Taxon synonym : allSynonymTypes) {
			if (MASTER_CHECKLIST.equals(synonym.getChecklist())) { // This taxon is used in the master checklist as a synonym
				if (allowsAlterationForUserDirectly(user, synonym)) {
					return true; // User has permissions to the master checklist taxon, so allow to edit this taxon
				}
				return false; // Do not allow to edit this taxon, because this is used in the master checklist as a synonym
			}
		}

		// This taxon is not from any checklist and not used in the master checklist as a synonym: 
		// If this user has edit permissions to one of the taxons that are a synonym of this taxon, allow to edit this taxon
		for (Taxon synonym : allSynonymTypes) {
			if (allowsAlterationForUserDirectly(user, synonym)) {
				return true; // User has permissions to the edit one of the synonyms of this taxon, allow to edit this
			}
		}
		
		return false;
	}

	private Collection<Taxon> getAllSynonyms(EditableTaxon editableTaxon) {
		List<Taxon> all = new ArrayList<>();
		all.addAll(editableTaxon.getSynonyms());
		all.addAll(editableTaxon.getMisappliedParents());
		all.addAll(editableTaxon.getUncertainSynonymParents());
		all.addAll(editableTaxon.getIncludedTaxa());
		all.addAll(editableTaxon.getIncludingTaxa());
		return Collections.unmodifiableCollection(all);
	}

	private boolean allowsAlterationForUserDirectly(User user, Taxon taxon) {
		if (SANDBOX_CHECKLIST.equals(taxon.getChecklist())) return true;
		return allowsAlterationForUserDirectly(user, taxon.getEditors());
	}

	private boolean allowsAlterationForUserDirectly(User user, Set<Qname> editors) {
		return editors.contains(user.getQname());
	}

	public void invalidate() {
		taxonContainer.invalidateTaxon(this);
	}

	private Boolean hasCriticalData = null;

	public boolean hasCriticalData() {
		if (hasCriticalData == null) { 
			hasCriticalData = initHasCriticalData();
		}
		return hasCriticalData;
	}

	private boolean initHasCriticalData() {
		if (this.hasChildren()) return true;
		if (this.hasSecureLevel()) return true;
		if (this.hasIUCNStatuses()) return true;
		if (!this.getAdministrativeStatuses().isEmpty()) return true;
		if (!this.getDescriptions().getContextsWithContentAndLocales().isEmpty()) return true;
		if (!this.getInvasiveSpeciesMainGroups().isEmpty()) return true;
		if (!this.getExplicitlySetEditors().isEmpty()) return true;
		if (!this.getExplicitlySetExperts().isEmpty()) return true;
		if (!this.getExplicitlySetInformalTaxonGroups().isEmpty()) return true;
		return false;
	}

	private boolean hasIUCNStatuses() {
		return getLatestRedListStatusFinland() != null;
	}

	public boolean hasExplicitlySetOriginalPublication(String qname) {
		return getExplicitlySetOriginalPublications().contains(new Qname(qname));
	}

	public boolean hasExplicitlySetOccurrenceInFinlandPublication(String qname) {
		return getExplicitlySetOccurrenceInFinlandPublications().contains(new Qname(qname));
	}

	public boolean hasExplicitlySetEditor(String qname) {
		return getExplicitlySetEditors().contains(new Qname(qname));
	}

	public boolean hasExplicitlySetExpert(String qname) {
		return getExplicitlySetExperts().contains(new Qname(qname));
	}

	public boolean hasExplicitlySetInformalTaxonGroup(String qname) {
		return getExplicitlySetInformalTaxonGroups().contains(new Qname(qname));
	}

}
