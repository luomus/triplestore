package fi.luomus.triplestore.taxonomy.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fi.luomus.commons.containers.Content;
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
		if (user.isAdmin()) {
			return true;
		}

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

		Collection<Taxon> allSynonymTypes = this.getAllSynonyms();

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

	private Collection<String> criticalDatas = null;
	
	public Collection<String> getCriticalData() {
		if (criticalDatas == null) {
			criticalDatas = initCriticalData();
		}
		return criticalDatas;
	}
	
	public boolean hasCriticalData() {
		return !getCriticalData().isEmpty();
	}

	private Collection<String> initCriticalData() {
		Collection<String> criticals = new ArrayList<>();
		if (this.hasChildren()) criticals.add("Taxon has children");
		if (this.hasSecureLevel()) criticals.add("Taxon has observation secure level");
		if (this.hasIUCNStatuses()) criticals.add("Taxon has an IUCN status");
		if (!this.getAdministrativeStatuses().isEmpty()) criticals.add("Taxon has an administrative status");
		if (!this.getDescriptions().getContextsWithContentAndLocales().isEmpty()) criticals.add("Taxon has description texts in " + contextNames(this.getDescriptions()));
		if (!this.getInvasiveSpeciesMainGroups().isEmpty()) criticals.add("Taxon is invasive species");
		if (!this.getExplicitlySetEditors().isEmpty()) criticals.add("Taxon is used to define editor permissions");
		if (!this.getExplicitlySetExperts().isEmpty()) criticals.add("Taxon is used to define expertise");
		if (!this.getExplicitlySetInformalTaxonGroups().isEmpty()) criticals.add("Taxon is set to an informal group");
		return criticals;
	}

	private String contextNames(Content descriptions) {
		Set<String> names = new HashSet<>();
		for (Qname context : descriptions.getContextsWithContentAndLocales().keySet()) {
			if (context.toString().startsWith("LA.")) {
				names.add("Pinkka");
			} else if (Content.DEFAULT_DESCRIPTION_CONTEXT.equals(context)) {
				names.add("FinBIF");
			} else {
				names.add(context.toURI());
			}
		}
		Iterator<String> i = names.iterator();
		StringBuilder b = new StringBuilder();
		while (i.hasNext()) {
			b.append(i.next());
			if (i.hasNext()) {
				b.append(", ");
			}
		}
		return b.toString();
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
