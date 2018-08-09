package fi.luomus.triplestore.taxonomy.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fi.luomus.commons.containers.Content;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.RedListStatus;
import fi.luomus.commons.taxonomy.Synonyms;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

public class EditableTaxon extends Taxon {

	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");

	private final CachedLiveLoadingTaxonContainer taxonContainer;
	private final ExtendedTaxonomyDAO taxonomyDAO;

	public EditableTaxon(Qname qname, CachedLiveLoadingTaxonContainer taxonContainer, ExtendedTaxonomyDAO taxonomyDAO) {
		super(qname, taxonContainer);
		this.taxonContainer = taxonContainer;
		this.taxonomyDAO = taxonomyDAO;
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

	public void invalidateSelfAndLinking() {
		taxonContainer.invalidateSelfAndLinking(this);
	}

	public void invalidateSelf() {
		taxonContainer.invalidateSelf(this);
	}

	private Boolean hasCritical = null;

	public boolean hasCriticalData() {
		if (hasCritical == null) hasCritical = initHasCritical();
		return hasCritical;
	}	

	public boolean hasTreeRelatedCriticalData() {
		if (this.hasExplicitlySetExpertsOrEditors()) return true;
		if (this.hasExplicitlySetHigherInformalTaxonGroup()) return true;
		return false;
	}

	private Boolean initHasCritical() {
		if (this.hasTreeRelatedCriticalData()) return true;
		if (this.hasChildren()) return true;
		if (this.hasSecureLevel()) return true;
		if (this.hasIUCNEvaluation()) return true;
		if (this.hasAdministrativeStatuses()) return true;
		if (this.hasDescriptions()) return true;
		if (this.isIdentifierUsedInDataWarehouse()) return true;
		return false;
	}

	public boolean hasExplicitlySetHigherInformalTaxonGroup() {
		if (this.isSpecies()) return false;
		return !this.getExplicitlySetInformalTaxonGroups().isEmpty();
	}

	public boolean hasExplicitlySetExpertsOrEditors() {
		if (!this.getExplicitlySetEditors().isEmpty()) return true;
		if (!this.getExplicitlySetExperts().isEmpty()) return true;
		return false;
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
		if (this.hasSynonyms()) return false;
		return !hasCriticalData();
	}

	public boolean isDetachable() {
		return !hasCriticalData();
	}

	public boolean hasIUCNEvaluation() {
		Collection<RedListStatus> statuses = this.getRedListStatusesInFinland();
		if (hasCriticalIUCNEvaluationStatus(statuses)) {
			return true;
		}
		try {
			IUCNContainer iucnContainer = taxonomyDAO.getIucnDAO().getIUCNContainer();
			iucnContainer.makeSureEvaluationDataIsLoaded();
			if (!iucnContainer.hasTarget(this.getQname().toString())) return false;
			IUCNEvaluationTarget target = iucnContainer.getTarget(this.getQname().toString());

			return hasCriticalIUCNEvaluationStatus(target);
		} catch (Exception e) {
			throw new RuntimeException(this.getQname().toString(), e);
		}
	}


	private boolean hasCriticalIUCNEvaluationStatus(IUCNEvaluationTarget target) {
		for (IUCNEvaluation e : target.getEvaluations()) {
			if (e.isCriticalDataEvaluation()) return true;
		}
		return false;
	}

	private boolean hasCriticalIUCNEvaluationStatus(Collection<RedListStatus> statuses) {
		for (RedListStatus s : statuses) {
			if (IUCNEvaluation.isCriticalIUCNEvaluation(s.getStatus().toString())) return true;
		}
		return false;
	}

	public boolean hasAdministrativeStatuses() {
		return !this.getAdministrativeStatuses().isEmpty();
	}

	public boolean hasDescriptions() {
		return !this.getDescriptions().getContextsWithContentAndLocales().isEmpty();
	}

	public String getContextNamesWithDescriptions() {
		Set<String> names = new HashSet<>();
		for (Qname context : this.getDescriptions().getContextsWithContentAndLocales().keySet()) {
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

	public boolean hasTaxonImages() {
		return taxonContainer.getHasMediaFilter().contains(this.getQname());
	}

	public boolean isIdentifierUsedInDataWarehouse() {
		return taxonomyDAO.isTaxonIdUsedInDataWarehouse(this.getQname());
	}

}
