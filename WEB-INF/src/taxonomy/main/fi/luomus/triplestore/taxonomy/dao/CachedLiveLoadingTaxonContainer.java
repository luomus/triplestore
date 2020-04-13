package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;

import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.taxonomy.Filter;
import fi.luomus.commons.taxonomy.InformalTaxonGroupContainer;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.taxonomy.TripletToTaxonHandler;
import fi.luomus.commons.taxonomy.TripletToTaxonHandlers;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
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

	private final TriplestoreDAO triplestoreDAO;
	private final ExtendedTaxonomyDAO taxonomyDAO;

	private final TripletToTaxonHandlers tripletToTaxonHandlers = new TripletToTaxonHandlers()
			.extend(Evaluation.PRIMARY_HABITAT, new TripletToTaxonHandler() {
				@Override
				public void setToTaxon(Qname context, Qname predicatename, Qname objectname, String resourceliteral, String locale, Taxon taxon) {
					EditableTaxon editableTaxon = (EditableTaxon) taxon;
					editableTaxon.setPrimaryHabitatId(objectname.toString());
				}
			})
			.extend(Evaluation.SECONDARY_HABITAT, new TripletToTaxonHandler() {
				@Override
				public void setToTaxon(Qname context, Qname predicatename, Qname objectname, String resourceliteral, String locale, Taxon taxon) {
					EditableTaxon editableTaxon = (EditableTaxon) taxon;
					if (editableTaxon.getSecondaryHabitatIds() == null) {
						editableTaxon.setSecondaryHabitatIds(new ArrayList<>());
					}
					editableTaxon.getSecondaryHabitatIds().add(objectname.toString());
				}
			});

	private final Cached<Qname, EditableTaxon> cachedTaxons = new Cached<>(new TaxonLoader(), 3, TimeUnit.HOURS, 25000, false);
	private final Cached<Qname, Set<Qname>> cachedChildren = new Cached<>(new ChildrenLoader(), 3, TimeUnit.HOURS, 25000, false);
	private final Cached<Qname, Optional<Qname>> cachedSynonymParents = new Cached<>(new SynonymParentLoader(), 3, TimeUnit.HOURS, 25000, false);
	private final SingleObjectCache<Set<Qname>> cachedTaxaWithImages = new SingleObjectCache<>(new TaxaWithImagesLoader(), 12, TimeUnit.HOURS);
	private final SingleObjectCache<Map<Qname, Set<Qname>>> cachedIucnGroupsOfInformalTaxonGroup; // informal taxon group id -> IUCN group ids
	private final SingleObjectCache<Map<Qname, Set<Qname>>> cachedIucnGroupsOfTaxon; // taxon id -> IUCN group ids
	private final SingleObjectCache<InformalTaxonGroupContainer> cachedInformalTaxonGroupContainer;
	private final SingleObjectCache<InformalTaxonGroupContainer> cachedRedListEvaluationGroupContainer;

	public CachedLiveLoadingTaxonContainer(TriplestoreDAO triplestoreDAO, final ExtendedTaxonomyDAO taxonomyDAO) {
		this.triplestoreDAO = triplestoreDAO;
		this.taxonomyDAO = taxonomyDAO;
		this.cachedIucnGroupsOfInformalTaxonGroup = new SingleObjectCache<>(new IucnGroupsOfInformalTaxonGroupLoader(taxonomyDAO) , 3, TimeUnit.HOURS);
		this.cachedIucnGroupsOfTaxon = new SingleObjectCache<>(new IucnGroupsOfTaxonLoader(taxonomyDAO) , 3, TimeUnit.HOURS);
		this.cachedInformalTaxonGroupContainer = new SingleObjectCache<>(new SingleObjectCache.CacheLoader<InformalTaxonGroupContainer>() {
			@Override
			public InformalTaxonGroupContainer load() {
				try {
					return new InformalTaxonGroupContainer(taxonomyDAO.getInformalTaxonGroupsForceReload());
				} catch (Exception e) {
					throw triplestoreDAO.exception("Informal taxon group loader", e);
				}
			}
		}, 5, TimeUnit.MINUTES);
		this.cachedRedListEvaluationGroupContainer = new SingleObjectCache<>(new SingleObjectCache.CacheLoader<InformalTaxonGroupContainer>() {
			@Override
			public InformalTaxonGroupContainer load() {
				try {
					return new InformalTaxonGroupContainer(taxonomyDAO.getRedListEvaluationGroupsForceReload());
				} catch (Exception e) {
					throw triplestoreDAO.exception("Red list evaluation group container loader", e);
				}
			}
		}, 5, TimeUnit.MINUTES);
	}

	private class IucnGroupsOfInformalTaxonGroupLoader implements SingleObjectCache.CacheLoader<Map<Qname, Set<Qname>>> {
		private final TaxonomyDAO dao;
		public IucnGroupsOfInformalTaxonGroupLoader(TaxonomyDAO dao) {
			this.dao = dao;
		}
		@Override
		public Map<Qname, Set<Qname>> load() {
			try {
				Map<Qname, Set<Qname>> map = new HashMap<>();
				for (RedListEvaluationGroup group : dao.getRedListEvaluationGroupsForceReload().values()) {
					for (Qname informalTaxonGroupId : group.getInformalGroups()) {
						if (!map.containsKey(informalTaxonGroupId)) {
							map.put(informalTaxonGroupId, new HashSet<Qname>());
						}
						map.get(informalTaxonGroupId).add(group.getQname());
					}
				}
				return map;
			} catch (Exception e) {
				throw triplestoreDAO.exception("Loading iucn groups of informal taxon groups", e);
			}
		}		
	}

	private class IucnGroupsOfTaxonLoader implements SingleObjectCache.CacheLoader<Map<Qname, Set<Qname>>> {
		private final TaxonomyDAO dao;
		public IucnGroupsOfTaxonLoader(TaxonomyDAO dao) {
			this.dao = dao;
		}
		@Override
		public Map<Qname, Set<Qname>> load() {
			try {
				Map<Qname, Set<Qname>> map = new HashMap<>();
				for (RedListEvaluationGroup group : dao.getRedListEvaluationGroupsForceReload().values()) {
					for (Qname taxonId : group.getTaxons()) {
						if (!map.containsKey(taxonId)) {
							map.put(taxonId, new HashSet<Qname>());
						}
						map.get(taxonId).add(group.getQname());
					}
				}
				return map;
			} catch (Exception e) {
				throw triplestoreDAO.exception("Loading iucn groups of taxons", e);
			}
		}		
	}

	private class TaxaWithImagesLoader implements SingleObjectCache.CacheLoader<Set<Qname>> {
		@Override
		public Set<Qname> load() {
			System.out.println("Loading taxa with media...");
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				Set<Qname> taxonIds = new HashSet<>();
				con = triplestoreDAO.openConnection();
				p = con.prepareStatement(" select distinct objectname from "+TriplestoreDAOConst.SCHEMA+".rdf_statementview where predicatename = 'MM.taxonURI' ");
				rs = p.executeQuery();
				rs.setFetchSize(4001);
				while (rs.next()) {
					taxonIds.add(new Qname(rs.getString(1)));
				}
				System.out.println("Taxa with media loaded.");
				return taxonIds;
			} catch (Exception e) {
				throw triplestoreDAO.exception("Taxa with media", e);
			} finally {
				Utils.close(p, rs, con);
			}
		}
	}

	private class TaxonLoader implements CacheLoader<Qname, EditableTaxon> {
		@Override
		public EditableTaxon load(Qname taxonQname) {
			try {
				EditableTaxon taxon = getOrNull(taxonQname);
				if (taxon != null) return taxon;
			} catch (Exception e) {
				throw triplestoreDAO.exception("Load taxon: " + taxonQname, e);
			}
			throw new NoSuchTaxonException(taxonQname);
		}

		private EditableTaxon getOrNull(Qname taxonQname) throws Exception {
			Model model = triplestoreDAO.get(taxonQname);
			if (model.isEmpty()) return null;
			EditableTaxon taxon = createTaxon(model);
			preloadSynonyms(taxon);
			taxon.getChildren(); // preload all child models to cache
			return taxon;
		}
	}

	private EditableTaxon createTaxon(Model model) {
		Qname qname = q(model.getSubject());
		EditableTaxon taxon = new EditableTaxon(qname, this, taxonomyDAO);
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
				for (Qname synonymId : taxon.getSynonymsContainer().getAll()) {
					cachedSynonymParents.put(synonymId, Optional.of(taxon.getQname()));
				}
				synonymIds.addAll(taxon.getSynonymsContainer().getAll());
			}
		}
		if (synonymIds.isEmpty()) return;

		for (Model synonymModel : triplestoreDAO.getSearchDAO().get(synonymIds)) {
			EditableTaxon synonym = createTaxon(synonymModel);
			cachedTaxons.put(synonym.getQname(), synonym);
		}
	}

	private Qname q(RdfResource resource) {
		return new Qname(resource.getQname());
	}

	private void addPropertyToTaxon(EditableTaxon taxon, Statement statement) {
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
				Collection<Model> models = triplestoreDAO.getSearchDAO().search(
						new SearchParams(Integer.MAX_VALUE, 0)
						.type("MX.taxon")
						.predicate(IS_PART_OF)
						.objectresource(taxonQname));
				if (models.isEmpty()) return Collections.emptySet();
				Set<Qname> childIds = new HashSet<>();
				List<EditableTaxon> createdChildren = new ArrayList<>();
				for (Model model : models) {
					EditableTaxon child = createTaxon(model);
					cachedTaxons.put(child.getQname(), child);
					childIds.add(child.getQname());
					createdChildren.add(child);
				}
				preloadSynonyms(createdChildren);
				return childIds;
			} catch (Exception e) {
				throw triplestoreDAO.exception("Children loader: " + taxonQname, e);
			}
		}
	}

	private class SynonymParentLoader implements CacheLoader<Qname, Optional<Qname>> {
		@Override
		public Optional<Qname> load(Qname synonymTaxonId) {
			try {
				Set<Qname> matches = triplestoreDAO.getSearchDAO().searchQnames(
						new SearchParams(Integer.MAX_VALUE, 0)
						.type("MX.taxon")
						.predicates(SYNONYM_PREDICATES)
						.objectresource(synonymTaxonId));
				if (matches.size() == 1) {
					return Optional.of(matches.iterator().next());
				}
				return Optional.absent();
			} catch (Exception e) {
				throw triplestoreDAO.exception("Synonym parent loader: " + synonymTaxonId, e);
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
		} catch (NoSuchTaxonException e) {
			return false;
		}
	}

	@Override
	public Set<Qname> getChildren(Qname parentId) {
		return cachedChildren.get(parentId);
	}

	@Override
	public Qname getSynonymParent(Qname synonymId) {
		return cachedSynonymParents.get(synonymId).orNull();
	}

	public void clearCaches() {
		synchronized (LOCK) {
			cachedChildren.invalidateAll();
			cachedSynonymParents.invalidateAll();
			cachedTaxons.invalidateAll();
			cachedTaxaWithImages.invalidate();
		}
	}

	public void invalidateSelf(Taxon taxon) {
		cachedTaxons.invalidate(taxon.getQname());
	}

	public void invalidateSelfAndLinking(Taxon taxon) {
		synchronized (LOCK) {
			if (taxon.isSynonym()) {
				Taxon synonymParent = taxon.getSynonymParent();
				if (synonymParent != null) {
					invalidateSelf(synonymParent);
				}
			}

			if (taxon.hasParent()) {
				cachedChildren.invalidate(taxon.getParentQname());
			}

			invalidateSelf(taxon);
		}
	}

	@Override
	public int getNumberOfTaxons() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getInformalGroupFilter() {
		return new Filter() {

			@Override
			public Set<Qname> getFilteredTaxons(Qname groupId) {
				try {
					return triplestoreDAO.getSearchDAO().searchQnames(new SearchParams(100, 0).predicate("MX.isPartOfInformalTaxonGroup").object(groupId.toString()));
				} catch (SQLException e) {
					e.printStackTrace();
					return Collections.emptySet();
				}
			}
		};
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
	public Filter getRedListEvaluationGroupFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getHasMediaFilter() {
		return cachedTaxaWithImages.get();
	}

	@Override
	public Set<Qname> getInvasiveSpeciesEarlyWarningFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getRedListEvaluationGroupsOfInformalTaxonGroup(Qname informalTaxonGroupId) {
		Set<Qname> set = cachedIucnGroupsOfInformalTaxonGroup.get().get(informalTaxonGroupId); 
		if (set == null) return Collections.emptySet();
		return set;
	}

	@Override
	public Set<Qname> getRedListEvaluationGroupsOfTaxon(Qname taxonId) {
		Set<Qname> set = cachedIucnGroupsOfTaxon.get().get(taxonId); 
		if (set == null) return Collections.emptySet();
		return set;
	}

	@Override
	public Set<Qname> orderInformalTaxonGroups(Set<Qname> informalTaxonGroupIds) {
		return cachedInformalTaxonGroupContainer.get().orderInformalTaxonGroups(informalTaxonGroupIds);
	}

	@Override
	public Set<Qname> getParentInformalTaxonGroups(Qname groupId) {
		return cachedInformalTaxonGroupContainer.get().getParents(groupId);
	}

	@Override
	public Set<Qname> getParentRedListEvaluationGroups(Qname groupId) {
		return cachedRedListEvaluationGroupContainer.get().getParents(groupId);
	}

	@Override
	public Collection<Taxon> getAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLatestLockedRedListEvaluationYear() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}

