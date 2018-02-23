package fi.luomus.triplestore.taxonomy.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.Filter;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.TripletToTaxonHandler;
import fi.luomus.commons.taxonomy.TripletToTaxonHandlers;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class CachedLiveLoadingTaxonContainer implements TaxonContainer {

	private static final Object LOCK = new Object();
	private static final String IS_PART_OF = TaxonContainer.IS_PART_OF.toString();
	private static final Set<String> SYNONYM_PREDICATES;
	static {
		SYNONYM_PREDICATES = new HashSet<>();
		for (Qname q : TaxonContainer.SYNONYM_PREDICATES) {
			SYNONYM_PREDICATES.add(q.toString());
		}
	}

	private final TripletToTaxonHandlers tripletToTaxonHandlers = new TripletToTaxonHandlers();
	private final Cached<Qname, EditableTaxon> cachedTaxons = new Cached<Qname, EditableTaxon>(new TaxonLoader(), 3*60*60, 25000);
	private final Cached<Qname, Set<Qname>> cachedChildren = new Cached<Qname, Set<Qname>>(new ChildrenLoader(), 3*60*60, 25000);
	private final Cached<Qname, Set<Qname>> cachedSynonymParents = new Cached<Qname, Set<Qname>>(new SynonymParentLoader(), 3*60*60, 25000);
	private final TriplestoreDAO triplestoreDAO;


	public CachedLiveLoadingTaxonContainer(TriplestoreDAO triplestoreDAO) {
		this.triplestoreDAO = triplestoreDAO;
	}

	private class TaxonLoader implements CacheLoader<Qname, EditableTaxon> {
		@Override
		public EditableTaxon load(Qname qname) {
			try {
				Model model = triplestoreDAO.get(qname);
				if (model.isEmpty()) throw new NoSuchTaxonException(qname);
				EditableTaxon taxon = createTaxon(model);
				preloadSynonyms(taxon);
				return taxon;
			} catch (NoSuchTaxonException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private EditableTaxon createTaxon(Model model) throws Exception {
		Qname qname = q(model.getSubject());
		System.out.println("Luotiin taksoni " + qname);
		EditableTaxon taxon = new EditableTaxon(qname, this);
		for (Statement statement : model.getStatements()) {
			addPropertyToTaxon(taxon, statement);
		}

		return taxon;
	}

	private void preloadSynonyms(EditableTaxon taxon) throws Exception {
		preloadSynonyms(Utils.list(taxon));
	}

	private void preloadSynonyms(List<EditableTaxon> taxons) throws Exception {
		Set<Qname> synonymIds = new HashSet<>();
		for (EditableTaxon taxon : taxons) {
			if (taxon.hasSynonyms()) {
				synonymIds.addAll(taxon.getSynonymsContainer().getAll());
			}
		}
		if (synonymIds.isEmpty()) return;
		System.out.println("Preload synonyms: " + synonymIds);
		
		Set<String> synonymQnames = new HashSet<>();
		for (Model synonymModel : triplestoreDAO.getSearchDAO().get(synonymIds)) {
			EditableTaxon synonym = createTaxon(synonymModel);
			cachedTaxons.put(synonym.getQname(), synonym);
			synonymQnames.add(synonym.getQname().toString());
		}

		System.out.println("Haetaan synonym parentit synonyymeille " + synonymQnames);
		Collection<Model> synonymParents = triplestoreDAO.getSearchDAO().search(
				new SearchParams()
				.type("MX.taxon")
				.predicates(SYNONYM_PREDICATES)
				.objectresources(synonymQnames));

		Map<Qname, Set<Qname>> synonymParentMap = new HashMap<>();
		for (Model synonymParent : synonymParents) {
			Qname synonymParentId = new Qname(synonymParent.getSubject().getQname());
			for (String predicate : SYNONYM_PREDICATES) {
				for (Statement statement : synonymParent.getStatements(predicate)) {
					Qname synonymId = new Qname(statement.getObjectResource().getQname());
					if (!synonymParentMap.containsKey(synonymId)) {
						synonymParentMap.put(synonymId, new HashSet<Qname>());
					}
					synonymParentMap.get(synonymId).add(synonymParentId);
				}
			}
		}
		for (Map.Entry<Qname, Set<Qname>> e : synonymParentMap.entrySet()) {
			cachedSynonymParents.put(e.getKey(), e.getValue());
			System.out.println("Taksonia ladatessa pistettiin " + e.getKey() + " <- " + e.getValue());
		}
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
			System.out.println("Ladataan lapsia " + taxonQname);
			try {
				Collection<Model> models = triplestoreDAO.getSearchDAO().search(
						new SearchParams()
						.type("MX.taxon")
						.predicate(IS_PART_OF)
						.objectresource(taxonQname));
				if (models.isEmpty()) return Collections.emptySet();
				Set<Qname> childTaxons = new HashSet<Qname>();
				List<EditableTaxon> createdChildren = new ArrayList<>();
				for (Model model : models) {
					EditableTaxon child = createTaxon(model);
					cachedTaxons.put(child.getQname(), child);
					childTaxons.add(child.getQname());
					createdChildren.add(child);
				}
				System.out.println("Ladattaessa " + taxonQname + " lapsia ladattiin " + childTaxons);
				preloadSynonyms(createdChildren);
				return childTaxons;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class SynonymParentLoader implements CacheLoader<Qname, Set<Qname>> {
		@Override
		public Set<Qname> load(Qname taxonQname) {
			System.out.println("Kutsutaan synonym parent loaderia " + taxonQname);
			try {
				return triplestoreDAO.getSearchDAO().searchQnames(
						new SearchParams()
						.type("MX.taxon")
						.predicates(SYNONYM_PREDICATES)
						.objectresource(taxonQname));
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
	public Set<Qname> getSynonymParents(Qname synonymId) {
		return cachedSynonymParents.get(synonymId);
	}

	public void clearCaches() {
		cachedChildren.invalidateAll();
		cachedSynonymParents.invalidateAll();
		cachedTaxons.invalidateAll();
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
		for (Taxon synonym : taxon.getAllSynonyms()) {
			invalidateTaxonInIsolation(synonym);
		}
		for (Taxon synonymParent : taxon.getSynonymParents()) {
			invalidateTaxonInIsolation(synonymParent);
		}
		if (taxon.hasParent()) {
			invalidateTaxonInIsolation(taxon.getParent());
		}
		cachedTaxons.invalidate(taxon.getQname());
		cachedChildren.invalidate(taxon.getQname());
		cachedSynonymParents.invalidate(taxon.getQname());
	}

	@Override
	public int getNumberOfTaxons() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
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

	@Override
	public Set<Qname> getHasDescriptionsFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getHasMediaFilter() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getInvasiveSpeciesEarlyWarningFilter() {
		throw new UnsupportedOperationException();
	}

}

