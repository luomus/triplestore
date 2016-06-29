package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

import java.util.ArrayList;
import java.util.List;

public class EditableTaxon extends Taxon {

	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");
	private static final Qname MASTER_CHECKLIST = new Qname("MR.1");


	private final ExtendedTaxonomyDAO taxonomyDAO;
	private final CachedLiveLoadingTaxonContainer taxonContainer;
	private final List<Qname> explicitlySetEditors = new ArrayList<>();
	private final List<Qname> explicitlySetExperts = new ArrayList<>();
	private final List<Qname> explicitlySetTaxonGroups = new ArrayList<>();
	private Qname parent;
	private Qname taxonConcept;
	private Qname checklistStatus;
	private Occurrences occurrences;

	public EditableTaxon(Qname qname, CachedLiveLoadingTaxonContainer taxonContainer, ExtendedTaxonomyDAO taxonomyDao) {
		super(qname, taxonContainer);
		this.taxonomyDAO = taxonomyDao;
		this.taxonContainer = taxonContainer;
	}

	public void setParentQname(Qname parent) {
		this.parent = parent;
	}

	@Override
	public Qname getParentQname() {
		return parent;
	}

	public void setTaxonConcept(Qname taxonConcept) {
		this.taxonConcept = taxonConcept;
	}

	@Override
	public Qname getTaxonConcept() {
		return taxonConcept;
	}

	@Override
	public int getCountOfSpecies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Taxon setChecklistStatus(Qname checklistStatus) {
		this.checklistStatus = checklistStatus;
		return this;
	}

	@Override
	public Qname getChecklistStatus() {
		return checklistStatus;
	}

	@Override
	public Taxon addExpert(Qname personQname) {
		this.explicitlySetExperts.add(personQname);
		return super.addExpert(personQname);
	}

	@Override
	public Taxon addEditor(Qname personQname) {
		this.explicitlySetEditors.add(personQname);
		return super.addEditor(personQname);
	}

	public List<Qname> getExplicitlySetEditors() {
		return this.explicitlySetEditors;
	}

	public List<Qname> getExplicitlySetExperts() {
		return this.explicitlySetExperts;
	}

	@Override
	public Taxon addInformalTaxonGroup(Qname groupQname) {
		explicitlySetTaxonGroups.add(groupQname);
		return super.addInformalTaxonGroup(groupQname);
	}

	public List<Qname> getExplicitlySetInformalTaxonGroups() {
		return this.explicitlySetTaxonGroups;
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
		for (Taxon synonym : this.getSynonymTaxons()) {
			if (MASTER_CHECKLIST.equals(synonym.getChecklist())) { // This taxon is used in the master checklist as a synonym
				if (allowsAlterationForUserDirectly(user, synonym)) {
					return true; // User has permissions to the master checklist taxon, so allow to edit this taxon
				}
				return false; // Do not allow to edit this taxon, because this is used in the master checklist as a synonym
			}
		}

		// This taxon is not from any checklist and not used in the master checklist as a synonym: 
		// If this user has edit permissions to one of the taxons that are a synonym of this taxon, allow to edit this taxon
		for (Taxon synonym : this.getSynonymTaxons()) {
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

	private boolean allowsAlterationForUserDirectly(User user, List<Qname> editors) {
		for (Qname editor : editors) {
			if (editor.equals(user.getQname())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Occurrences getOccurrences() {
		if (occurrences == null) {
			occurrences = new Occurrences(getQname());
			taxonomyDAO.addOccurrences(this);
		}
		return occurrences;
	}

	public void invalidate() {
		taxonContainer.invalidateTaxon(this);
	}



}
