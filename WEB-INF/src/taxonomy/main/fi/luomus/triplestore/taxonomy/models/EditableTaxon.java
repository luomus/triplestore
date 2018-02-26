package fi.luomus.triplestore.taxonomy.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fi.luomus.commons.containers.Content;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Synonyms;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

public class EditableTaxon extends Taxon {

	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");

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

		if (!this.isSynonym()) {
			return false; // This is a taxon in some checklist: editor has to have permissions to edit this taxon in that checklist  
		}

		Taxon synonymParent = this.getSynonymParent();
		if (synonymParent == null) return false;

		return allowsAlterationForUserDirectly(user, synonymParent);
	}

	private boolean allowsAlterationForUserDirectly(User user, Taxon taxon) {
		if (SANDBOX_CHECKLIST.equals(taxon.getChecklist())) return true;
		return allowsAlterationForUserDirectly(user, taxon.getEditors());
	}

	private boolean allowsAlterationForUserDirectly(User user, Set<Qname> editors) {
		return editors.contains(user.getQname());
	}

	@Override
	public Synonyms getSynonymsContainer() {
		return super.getSynonymsContainer();
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

	@Override
	public boolean isFinnish() {
		return this.isMarkedAsFinnishTaxon();
	}

	public boolean isDeletable() {
		long lastAllowed = TaxonomyEditorBaseServlet.getLastAllowedTaxonDeleteTimestamp();
		if (this.getCreatedAtTimestamp() < lastAllowed) return false;
		return !hasCriticalData();
	}

}
