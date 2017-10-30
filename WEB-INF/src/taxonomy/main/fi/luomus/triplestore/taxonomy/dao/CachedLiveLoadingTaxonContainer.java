package fi.luomus.triplestore.taxonomy.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.Filter;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonConcept;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.TripletToTaxonHandler;
import fi.luomus.commons.taxonomy.TripletToTaxonHandlers;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class CachedLiveLoadingTaxonContainer implements TaxonContainer {

	private static final String MX_BASIONYM_CIRCUMSCRIPTION = "MX.basionymCircumscription";
	private static final String MX_MISSPELLED_CIRCUMSCRIPTION = "MX.misspelledCircumscription";
	private static final String MX_UNCERTAIN_CIRCUMSCRIPTION = "MX.uncertainCircumscription";
	private static final String MX_MISAPPLIED_CIRCUMSCRIPTION = "MX.misappliedCircumscription";
	private static final String MX_CIRCUMSCRIPTION = "MX.circumscription";
	private static final String MX_IS_PART_OF = "MX.isPartOf";

	private static final Object LOCK = new Object();

	private final TripletToTaxonHandlers tripletToTaxonHandlers = new TripletToTaxonHandlers();
	private final Cached<Qname, EditableTaxon> cachedTaxons = new Cached<Qname, EditableTaxon>(new TaxonLoader(), 3*60*60, 25000);
	private final Cached<Qname, Set<Qname>> cachedChildren = new Cached<Qname, Set<Qname>>(new ChildrenLoader(), 3*60*60, 25000);
	private final Cached<Qname, TaxonConcept> cachedTaxonConcepts = new Cached<Qname, TaxonConcept>(new TaxonConceptLoader(), 3*60*60, 25000);
	private final SingleObjectCache<ConceptIncludes> cachedTaxonConceptIncludes = new SingleObjectCache<>(new TaxonConceptIncludesLoader(), 1*60*60);
	private final TriplestoreDAO triplestoreDAO;

	private static class ConceptIncludes {
		private final Map<Qname, Set<Qname>> incudedIn = new HashMap<>();
		private final Map<Qname, Set<Qname>> incudes = new HashMap<>();
	}
	
	public CachedLiveLoadingTaxonContainer(TriplestoreDAO triplestoreDAO) {
		this.triplestoreDAO = triplestoreDAO;
	}

	private class TaxonLoader implements CacheLoader<Qname, EditableTaxon> {
		@Override
		public EditableTaxon load(Qname qname) {
			try {
				Model model = triplestoreDAO.get(qname);
				if (model.isEmpty()) throw new NoSuchTaxonException(qname);
				return createTaxon(model);
			} catch (NoSuchTaxonException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public EditableTaxon createTaxon(Model model) {
		Qname qname = q(model.getSubject());
		EditableTaxon taxon = new EditableTaxon(qname, this);
		for (Statement statement : model.getStatements()) {
			String predicate = statement.getPredicate().getQname();
			Qname object = q(statement);
			if (MX_IS_PART_OF.equals(predicate)) {
				taxon.setParentQname(object);
			} else if (MX_CIRCUMSCRIPTION.equals(predicate)) {
				taxon.setTaxonConceptQname(object);
			} else if (MX_MISAPPLIED_CIRCUMSCRIPTION.equals(predicate)) {
				taxon.usedAsMisappliedSynonymInConcept(object);
			} else if (MX_UNCERTAIN_CIRCUMSCRIPTION.equals(predicate)) {
				taxon.usedAsUncertainSynonymInConcept(object);
			} else if (MX_MISSPELLED_CIRCUMSCRIPTION.equals(predicate)) {
				taxon.usedAsMisspelledSynonymInConcept(object);
			} else if (MX_BASIONYM_CIRCUMSCRIPTION.equals(predicate)) {
				taxon.usedAsBasionymSynonymInConcept(object);
			} else {
				addPropertyToTaxon(taxon, statement);
			}
		}
		return taxon;
	}

	private Qname q(Statement statement) {
		if (statement.isResourceStatement()) {
			return q(statement.getObjectResource());
		}
		return null;
	}

	private Qname q(RdfResource resource) {
		return new Qname(resource.getQname());
	}

	private void addPropertyToTaxon(Taxon taxon, Statement statement) {
		Qname context = statement.isForDefaultContext() ? null : q(statement.getContext());
		Qname predicatename = q(statement.getPredicate());
		Qname objectname = null;
		String resourceliteral = null;
		String langcode = null;

		if (statement.isLiteralStatement()) {
			resourceliteral = statement.getObjectLiteral().getContent();
			langcode = statement.getObjectLiteral().getLangcode();
			if (!given(langcode)) langcode = null;
		} else {
			objectname = q(statement.getObjectResource());
		}

		TripletToTaxonHandler handler = tripletToTaxonHandlers.getHandler(predicatename);
		handler.setToTaxon(context, predicatename, objectname, resourceliteral, langcode, taxon);
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	private class ChildrenLoader implements CacheLoader<Qname, Set<Qname>> {
		@Override
		public Set<Qname> load(Qname taxonQname) {
			try {
				Set<Qname> childTaxons = new HashSet<Qname>();
				Collection<Model> models = triplestoreDAO.getSearchDAO().search(MX_IS_PART_OF, taxonQname.toString());
				for (Model model : models) {
					EditableTaxon child = createTaxon(model);
					cachedTaxons.put(child.getQname(), child);
					childTaxons.add(child.getQname());
				}
				return childTaxons;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class TaxonConceptLoader implements CacheLoader<Qname, TaxonConcept> {
		@Override
		public TaxonConcept load(Qname conceptQname) {
			try {
				TaxonConcept taxonConcept = new TaxonConcept(conceptQname);
				
				Collection<Model> models = getSynonymTaxonModels(conceptQname, MX_CIRCUMSCRIPTION);
				for (Model model : models) {
					EditableTaxon taxon = createTaxon(model);
					cachedTaxons.put(taxon.getQname(), taxon);
					taxonConcept.setPartOfConcept(taxon.getQname());
				}
				
				models = getSynonymTaxonModels(conceptQname, MX_UNCERTAIN_CIRCUMSCRIPTION);
				for (Model model : models) {
					EditableTaxon taxon = createTaxon(model);
					cachedTaxons.put(taxon.getQname(), taxon);
					taxonConcept.setUncertain(taxon.getQname());
				}
				
				models = getSynonymTaxonModels(conceptQname, MX_MISAPPLIED_CIRCUMSCRIPTION);
				for (Model model : models) {
					EditableTaxon taxon = createTaxon(model);
					cachedTaxons.put(taxon.getQname(), taxon);
					taxonConcept.setMisapplied(taxon.getQname());
				}
				
				models = getSynonymTaxonModels(conceptQname, MX_MISSPELLED_CIRCUMSCRIPTION);
				for (Model model : models) {
					EditableTaxon taxon = createTaxon(model);
					cachedTaxons.put(taxon.getQname(), taxon);
					taxonConcept.setMisspelled(taxon.getQname());
				}
				
				models = getSynonymTaxonModels(conceptQname, MX_BASIONYM_CIRCUMSCRIPTION);
				for (Model model : models) {
					EditableTaxon taxon = createTaxon(model);
					cachedTaxons.put(taxon.getQname(), taxon);
					taxonConcept.setBasionym(taxon.getQname());
				}
				
				ConceptIncludes conceptIncludes = cachedTaxonConceptIncludes.get();
				if (conceptIncludes.incudedIn.containsKey(conceptQname)) {
					for (Qname includedIn : conceptIncludes.incudedIn.get(conceptQname)) {
						taxonConcept.setIncludedIn(includedIn);
					}
				}
				if (conceptIncludes.incudes.containsKey(conceptQname)) {
					for (Qname includes : conceptIncludes.incudes.get(conceptQname)) {
						taxonConcept.setIncludes(includes);
					}
				}
				return taxonConcept;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Collection<Model> getSynonymTaxonModels(Qname conceptQname, String predicate) throws Exception {
			Collection<Model> models = triplestoreDAO.getSearchDAO().search(
					new SearchParams(Integer.MAX_VALUE, 0)
					.type("MX.taxon")
					.predicate(predicate)
					.objectresource(conceptQname.toString()));
			return models;
		}
	}

	private class TaxonConceptIncludesLoader implements SingleObjectCache.CacheLoader<ConceptIncludes> {
		private static final String MC_INCLUDED_IN = "MC.includedIn";

		@Override
		public ConceptIncludes load() {
			try {
				ConceptIncludes conceptIncludes = new ConceptIncludes();
				Collection<Model> models = triplestoreDAO.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).predicate(MC_INCLUDED_IN));
				for (Model model : models) {
					Qname includedConceptQname = q(model.getSubject());
					if (!conceptIncludes.incudedIn.containsKey(includedConceptQname)) {
						conceptIncludes.incudedIn.put(includedConceptQname, new HashSet<Qname>());
					}
					for (Statement s : model.getStatements(MC_INCLUDED_IN)) {
						if (!s.isResourceStatement()) continue;
						Qname includingConceptQname = q(s.getObjectResource());
						conceptIncludes.incudedIn.get(includedConceptQname).add(includingConceptQname);
						if (!conceptIncludes.incudes.containsKey(includingConceptQname)) {
							conceptIncludes.incudes.put(includingConceptQname, new HashSet<Qname>());
						}
						conceptIncludes.incudes.get(includingConceptQname).add(includedConceptQname);
					}
				}
				return conceptIncludes;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public EditableTaxon getTaxon(Qname taxonId) throws NoSuchTaxonException {
		return cachedTaxons.get(taxonId);
	}

	@Override
	public boolean hasTaxon(Qname taxonId) {
		try {
			cachedTaxons.get(taxonId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Set<Qname> getChildren(Qname parentId) {
		return cachedChildren.get(parentId);
	}
	
	@Override
	public TaxonConcept getTaxonConcept(Qname taxonConceptQname) {
		if (!given(taxonConceptQname)) return null;
		return cachedTaxonConcepts.get(taxonConceptQname);		
	}

	private boolean given(Qname qname) {
		return qname != null && qname.isSet();
	}

	@Override
	public Filter getInformalGroupFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getAdminStatusFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getRedListStatusFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getTypesOfOccurrenceFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getInvasiveSpeciesFilter() {
		throw new UnsupportedOperationException();
	}

	public void clearCaches() {
		cachedChildren.invalidateAll();
		cachedTaxonConcepts.invalidateAll();
		cachedTaxons.invalidateAll();
		cachedTaxonConceptIncludes.invalidate();
	}

	public void clearTaxonConceptLinkings() {
		cachedTaxonConceptIncludes.getForceReload();
		cachedTaxonConcepts.invalidateAll();
	}
	
	public void invalidateTaxon(Taxon taxon) {
		synchronized (LOCK) {
			alreadyInvalidatedTaxonsInIsolation.clear();
			invalidateTaxonInIsolation(taxon);
			alreadyInvalidatedTaxonsInIsolation.clear();
		}

	}

	private final Set<Qname> alreadyInvalidatedTaxonsInIsolation = new HashSet<>();

	private void invalidateTaxonInIsolation(Taxon taxon) {
		if (alreadyInvalidatedTaxonsInIsolation.contains(taxon.getQname())) return;
		alreadyInvalidatedTaxonsInIsolation.add(taxon.getQname());
		for (Taxon synonym : taxon.getSynonyms()) {
			invalidateTaxonInIsolation(synonym);
		}
		if (taxon.hasParent()) {
			invalidateTaxonInIsolation(taxon.getParent());
		}
		cachedTaxons.invalidate(taxon.getQname());
		cachedChildren.invalidate(taxon.getQname());
		if (taxon.getTaxonConceptQname() != null) {
			cachedTaxonConcepts.invalidate(taxon.getTaxonConceptQname());
		}
	}

}

