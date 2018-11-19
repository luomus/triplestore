package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;
import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchDAOSQLQueryImple;
import fi.luomus.commons.taxonomy.TaxonSearchDataSourceDefinition;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonomyDAOBaseImple;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class ExtendedTaxonomyDAOImple extends TaxonomyDAOBaseImple implements ExtendedTaxonomyDAO {

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private final TriplestoreDAO triplestoreDAO;
	private final IucnDAOImple iucnDAO;
	private final CachedLiveLoadingTaxonContainer taxonContainer;
	private final Cached<TaxonSearch, TaxonSearchResponse> cachedTaxonSearches;
	private final Cached<Qname, Boolean> cachedDwUses;
	private final DataSource dataSource;
	private final Config config;

	public ExtendedTaxonomyDAOImple(Config config, TriplestoreDAO triplestoreDAO, ErrorReporter errorReporter) {
		this(config, config.developmentMode(), triplestoreDAO, errorReporter);
	}

	public ExtendedTaxonomyDAOImple(Config config, boolean devMode, TriplestoreDAO triplestoreDAO, ErrorReporter errorReporter) {
		super(config, 3* 60 * 60, 10 * 60);
		System.out.println("Creating " +  ExtendedTaxonomyDAOImple.class.getName());
		this.dataSource = TaxonSearchDataSourceDefinition.initDataSource(config.connectionDescription());
		this.triplestoreDAO = triplestoreDAO;
		this.taxonContainer = new CachedLiveLoadingTaxonContainer(triplestoreDAO, this);
		this.iucnDAO = new IucnDAOImple(config, devMode, triplestoreDAO, this, errorReporter);
		this.cachedTaxonSearches = new Cached<TaxonSearch, TaxonSearchResponse>(
				new TaxonSearchLoader(),
				60*60*3, 50000);
		this.cachedDwUses = new Cached<Qname, Boolean>(
				new DwUseLoader(),
				60*60*3, 50000);
		this.config = config;
	}

	private class DwUseLoader implements Cached.CacheLoader<Qname, Boolean> {
		@Override
		public Boolean load(Qname taxonId) {
			HttpClientService client = null;
			try {
				String dwUri = config.get("DwURL");
				client = new HttpClientService();
				JSONObject response = client.contentAsJson(new HttpGet(dwUri+"/"+taxonId.toString()));
				if (response.hasKey("used")) {
					return response.getBoolean("used");
				}
				if (response.hasKey("status")) {
					throw new Exception(dwUri + " returned status: " + response.getInteger("status") + " with message: " + response.getString("message"));
				}
				throw new Exception(dwUri + " returned unknown response");
			} catch (Exception e) {
				throw new RuntimeException("Dw use for " + taxonId, e);
			} finally {
				client.close();
			}
		}
	}

	private class TaxonSearchLoader implements Cached.CacheLoader<TaxonSearch, TaxonSearchResponse> {
		@Override
		public TaxonSearchResponse load(TaxonSearch key) {
			try {
				return uncachedTaxonSearch(key);
			} catch (Exception e) {
				throw new RuntimeException("Taxon search with terms " + key.toString(), e);
			}
		}
	}

	public void close() {
		if (iucnDAO != null) iucnDAO.close();
		if (dataSource != null) dataSource.close();
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		taxonContainer.clearCaches();
	}

	@Override
	public EditableTaxon getTaxon(Qname qname) {
		return taxonContainer.getTaxon(qname);
	}

	@Override
	public void addHabitats(EditableTaxon taxon) {
		try {
			if (taxon.getPrimaryHabitat() != null) return;
			Set<String> habitatObjectIds = taxon.getHabitatIds();
			if (habitatObjectIds.isEmpty()) return;

			Collection<Model> models = triplestoreDAO.getSearchDAO().search(new SearchParams(1000, 0).subjects(habitatObjectIds));
			for (Model model : models) {
				HabitatObject h = IucnDAOImple.constructHabitatObject(model);
				if (h.getId().toString().equals(taxon.getPrimaryHabitatId())) {
					taxon.setPrimaryHabitat(h);
				} else {
					taxon.addSecondaryHabitat(h);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addOccurrences(EditableTaxon taxon) {
		try {
			Collection<Model> models = triplestoreDAO.getSearchDAO().search(
					new SearchParams(1000, 0)
					.type("MO.occurrence")
					.predicate("MO.taxon")
					.objectresource(taxon.getQname().toString()));
			for (Model model : models) {
				Qname id = q(model.getSubject());
				Qname area = null;
				Qname status = null;
				String notes = null;
				Integer year = null;
				for (Statement s : model.getStatements()) {
					if (s.getPredicate().getQname().equals("MO.area")) {
						area = q(s.getObjectResource());
					} else if (s.getPredicate().getQname().equals("MO.status")) {
						status = q(s.getObjectResource());
					} else if (s.getPredicate().getQname().equals("MO.notes")) {
						notes = s.getObjectLiteral().getContent();
					} else if (s.getPredicate().getQname().equals("MO.year")) {
						try {
							year = Integer.valueOf(s.getObjectLiteral().getContent());
						} catch (Exception e) {}
					}
				}
				Occurrence occurrence = new Occurrence(id, area, status);
				occurrence.setNotes(notes);
				occurrence.setYear(year);
				taxon.getOccurrences().setOccurrence(occurrence);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Qname q(RdfResource resource) {
		return new Qname(resource.getQname());
	}


	private TaxonSearchResponse uncachedTaxonSearch(TaxonSearch taxonSearch) throws Exception {
		return new TaxonSearchDAOSQLQueryImple(this, dataSource, TriplestoreDAOConst.SCHEMA).search(taxonSearch);
	}

	@Override
	public TaxonSearchResponse search(TaxonSearch taxonSearch) throws Exception {
		return cachedTaxonSearches.get(taxonSearch);
	}

	@Override
	public IucnDAO getIucnDAO() {
		return iucnDAO;
	}

	@Override
	public CachedLiveLoadingTaxonContainer getTaxonContainer() throws Exception {
		return taxonContainer;
	}

	@Override
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Taxon taxon) throws Exception {
		List<Taxon> matches = new ArrayList<Taxon>();

		Qname checklist = null;
		if (given(taxon.getChecklist())) {
			checklist = taxon.getChecklist();
		} else {
			Taxon synonymParent = taxon.getSynonymParent();
			if (synonymParent != null) {
				checklist = synonymParent.getChecklist();
			}
			if (!given(checklist)) {
				checklist = new Qname("MR.1");
			}
		}

		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = triplestoreDAO.openConnection();
			p = con.prepareStatement("" +
					" SELECT qname, scientificname, author, taxonrank FROM "+SCHEMA+".taxon_search_materialized " +
					" WHERE checklist = ? AND name = ? AND qname != ? ");
			p.setString(1, checklist.toString());
			p.setString(2, name.toUpperCase());
			p.setString(3, taxon.getQname().toString());
			rs = p.executeQuery();
			rs.setFetchSize(4001);
			while (rs.next()) {
				Qname matchQname = new Qname(rs.getString(1));
				String matchScientificName = rs.getString(2);
				String matchAuthor = rs.getString(3);
				String matchRank = rs.getString(4);
				Taxon match = new Taxon(matchQname, null);
				match.setScientificName(matchScientificName);
				match.setScientificNameAuthorship(matchAuthor);
				if (given(matchRank)) {
					match.setTaxonRank(new Qname(matchRank));
				}
				matches.add(match);
			}
		} finally {
			Utils.close(p, rs, con);
		}
		return matches;
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	@Override
	public EditableTaxon createTaxon() throws Exception {
		Qname qname = triplestoreDAO.getSeqNextValAndAddResource("MX");
		return new EditableTaxon(qname, taxonContainer, this);
	}

	@Override
	public Set<String> getInformalTaxonGroupRoots() {
		return getRoots(getInformalTaxonGroups());
	}

	@Override
	public Set<String> getIucnRedListInformalGroupRoots() {
		return getRoots(getIucnRedListInformalTaxonGroups());
	}

	private <T extends InformalTaxonGroup> Set<String> getRoots(Map<String, T> allGroups) {
		Set<String> roots = new LinkedHashSet<>(allGroups.keySet());
		for (T group : allGroups.values()) {
			for (Qname subGroup : group.getSubGroups()) {
				roots.remove(subGroup.toString());
			}
		}
		return roots;
	}

	private final SingleObjectCache<Map<String, Area>> cachedBiogeographicalProvinces = 
			new SingleObjectCache<Map<String, Area>>(
					new CacheLoader<Map<String, Area>>() {
						private final Qname BIOGEOGRAPHICAL_PROVINCE = new Qname("ML.biogeographicalProvince");
						@Override
						public Map<String, Area> load() {
							try {
								Map<String, Area> areas = new LinkedHashMap<>();
								for (Area area : getAreas().values()) {
									if (BIOGEOGRAPHICAL_PROVINCE.equals(area.getType())) {
										areas.put(area.getQname().toString(), area);
									}
								}
								return areas;
							} catch (Exception e) {
								throw new RuntimeException("Loading biogeographical provinces", e);
							}
						}
					}, 60*60*7);

	@Override
	public Map<String, Area> getBiogeographicalProvinces() throws Exception {
		return cachedBiogeographicalProvinces.get();
	}

	@Override
	public boolean isTaxonIdUsedInDataWarehouse(Qname taxonId) {
		return cachedDwUses.get(taxonId);
	}

}
