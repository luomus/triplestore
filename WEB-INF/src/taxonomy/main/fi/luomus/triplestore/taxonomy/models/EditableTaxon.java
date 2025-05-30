package fi.luomus.triplestore.taxonomy.models;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fi.luomus.commons.containers.Content;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.PublicInformation;
import fi.luomus.commons.taxonomy.RedListStatus;
import fi.luomus.commons.taxonomy.Synonyms;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

public class EditableTaxon extends Taxon {

	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");

	private final CachedLiveLoadingTaxonContainer taxonContainer;
	private final ExtendedTaxonomyDAO taxonomyDAO;
	private String primaryHabitatId;
	private List<String> secondaryHabitatIds;

	public EditableTaxon(Qname qname, CachedLiveLoadingTaxonContainer taxonContainer, ExtendedTaxonomyDAO taxonomyDAO) {
		super(qname, taxonContainer);
		this.taxonContainer = taxonContainer;
		this.taxonomyDAO = taxonomyDAO;
	}

	@Override
	public int getCountOfSpecies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getObservationCountFinland() {
		return 0;
	}

	@Override
	public boolean isHidden() {
		return super.isMarkedHidden(); // super implementation marks all higher taxa that do not have species as hidden
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
	public Synonyms getSynonymsContainer() { // change visibility to public
		return super.getSynonymsContainer();
	}

	public void invalidateSelfAndLinking() {
		taxonContainer.invalidateSelfAndLinking(this);
	}

	public void invalidateSelf() {
		taxonContainer.invalidateSelf(this);
	}

	private Boolean hasCritical = null;

	public boolean allowsMoveAsSynonym() {
		return this.getChecklist() == null;
	}

	public boolean allowsMoveAsChild() {
		return this.getChecklist() != null && !hasTreeRelatedCriticalData();
	}

	public boolean hasCriticalData() {
		if (hasCritical == null) hasCritical = initHasCritical();
		return hasCritical;
	}

	public boolean hasTreeRelatedCriticalData() {
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

	public boolean hasOriginalDescription(String qname) {
		if (super.getOriginalDescription() == null) return false;
		return super.getOriginalDescription().toString().equals(qname);
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
			Container iucnContainer = taxonomyDAO.getIucnDAO().getIUCNContainer();
			iucnContainer.makeSureEvaluationDataIsLoaded();
			if (!iucnContainer.hasTarget(this.getQname().toString())) return false;
			EvaluationTarget target = iucnContainer.getTarget(this.getQname().toString());

			return hasCriticalIUCNEvaluationStatus(target);
		} catch (Exception e) {
			throw new RuntimeException(this.getQname().toString(), e);
		}
	}


	private boolean hasCriticalIUCNEvaluationStatus(EvaluationTarget target) {
		for (Evaluation e : target.getEvaluations()) {
			if (e.isCriticalDataEvaluation()) return true;
		}
		return false;
	}

	private boolean hasCriticalIUCNEvaluationStatus(Collection<RedListStatus> statuses) {
		for (RedListStatus s : statuses) {
			if (Evaluation.isCriticalIUCNEvaluation(s.getStatus().toString())) return true;
		}
		return false;
	}

	public boolean hasAdministrativeStatuses() {
		return !this.getAdministrativeStatuses().isEmpty();
	}

	@Override
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

	public String getPrimaryHabitatId() {
		return primaryHabitatId;
	}

	public void setPrimaryHabitatId(String primaryHabitatId) {
		this.primaryHabitatId = primaryHabitatId;
	}

	public List<String> getSecondaryHabitatIds() {
		return secondaryHabitatIds;
	}

	public void setSecondaryHabitatIds(List<String> secondaryHabitatIds) {
		this.secondaryHabitatIds = secondaryHabitatIds;
	}

	public Set<String> getHabitatIds() {
		if (primaryHabitatId == null && secondaryHabitatIds == null) return Collections.emptySet();
		Set<String> ids = new HashSet<>();
		if (primaryHabitatId != null) ids.add(primaryHabitatId);
		if (secondaryHabitatIds != null) ids.addAll(secondaryHabitatIds);
		return ids;
	}

	private Boolean containsOrIsFinnishTaxon = null;

	@Override
	@PublicInformation(order=5001)
	public boolean isFinnish() {
		if (containsOrIsFinnishTaxon == null) {
			containsOrIsFinnishTaxon = isFinnish(3);
		}
		return containsOrIsFinnishTaxon;
	}

	private boolean isFinnish(int maxLevelToCheck) {
		if (this.isMarkedAsFinnishTaxon()) {
			this.containsOrIsFinnishTaxon = true;
			return true;
		}

		maxLevelToCheck--;
		if (maxLevelToCheck <= 0) return false; // give up -  don't store result to containsOrIsFinnishTaxon

		List<Taxon> children = this.getChildren();
		if (children.isEmpty()) {
			this.containsOrIsFinnishTaxon = false;
			return false;
		}

		for (Taxon child : children) {
			if (child.isMarkedAsFinnishTaxon()) {
				this.containsOrIsFinnishTaxon = true;
				return true;
			}
		}

		if (children.size() > 50) return false; // give up - don't store result to containsOrIsFinnishTaxon

		for (Taxon child : children) {
			EditableTaxon eChild = (EditableTaxon) child;
			if (eChild.isFinnish(maxLevelToCheck)) {
				this.containsOrIsFinnishTaxon = true;
				return true;
			}
		}

		return false; // going through children possibly gave up so don't store result to containsOrIsFinnishTaxon
	}

}
