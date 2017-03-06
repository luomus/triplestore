package fi.luomus.triplestore.taxonomy.dao;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAOBaseImple;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse.Match;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtendedTaxonomyDAOImple extends TaxonomyDAOBaseImple implements ExtendedTaxonomyDAO {

	private static final double JARO_WINKLER_DISTANCE = 0.88;
	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String TAXON_SEARCH_LIKELY_MATCH_SQL = "" +
			" SELECT   qname, name, utl_match.jaro_winkler(name, ?) match " +
			" FROM     "+SCHEMA+".taxon_search_materialized   " +
			" WHERE    utl_match.jaro_winkler(name, ?) > " + JARO_WINKLER_DISTANCE +
			" AND      name != ? " + 
			" AND      COALESCE(checklist, '.') = ? " + 
			" ORDER BY match DESC, name  ";

	private static final String TAXON_SEARCH_PARTIAL_MATCH_SQL = "" +
			" SELECT   qname, name " +
			" FROM     "+SCHEMA+".taxon_search_materialized   " +
			" WHERE    name LIKE ? " +
			" AND      name != ? " +
			" AND      COALESCE(checklist, '.') = ? " + 
			" ORDER BY name  ";

	private static final String TAXON_SEARCH_EXACT_MATCH_SQL = "" +
			" SELECT   qname, name " +
			" FROM     "+SCHEMA+".taxon_search_materialized                     " +
			" WHERE    (name = ? AND COALESCE(checklist, '.') = ? )              " +
			" OR       qname = ?                                                 ";

	private final TriplestoreDAO triplestoreDAO;
	private final IucnDAO iucnDAO;
	private final CachedLiveLoadingTaxonContainer taxonContainer;

	public ExtendedTaxonomyDAOImple(Config config, TriplestoreDAO triplestoreDAO, ErrorReporter errorReporter) {
		super(config, 60 * 5, 20);
		this.triplestoreDAO = triplestoreDAO;
		this.iucnDAO = new IucnDAOImple(config, triplestoreDAO, this, errorReporter);
		this.taxonContainer = new CachedLiveLoadingTaxonContainer(triplestoreDAO);
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

	@Override
	public Document search(TaxonSearch taxonSearch) throws Exception {
		return searchInternal(taxonSearch).getResultsAsDocument();
	}

	@Override
	public TaxonSearchResponse searchInternal(TaxonSearch taxonSearch) throws Exception {
		TaxonSearchResponse response = new TaxonSearchResponse();
		if (!given(taxonSearch.getSearchword())) {
			response.setError("Search word must be given.");
			return response;
		}
		String searchword = taxonSearch.getSearchword().trim().toUpperCase();
		if (searchword.trim().length() <= 1) {
			response.setError("Search word was too short.");
			return response;
		}
		String checklist = ".";
		if (taxonSearch.hasChecklist()) {
			checklist = taxonSearch.getChecklist().toString().trim().toUpperCase();
		}
		int limit = taxonSearch.getLimit();
		int addedCount = 0;
		TransactionConnection con = null;
		try {
			con = triplestoreDAO.openConnection();

			List<Match> exactMatches = exactMatches(searchword, checklist, limit, taxonSearch, con);
			response.getExactMatches().addAll(exactMatches);
			addedCount = exactMatches.size();
			if (addedCount >= limit) return response;

			if (taxonSearch.isOnlyExact() || searchword.length() <= 3) return response;

			List<Match> likelyMatches = likelyMatches(searchword, checklist, limit - addedCount, taxonSearch, con);
			response.getLikelyMatches().addAll(likelyMatches);
			addedCount += likelyMatches.size();
			if (addedCount >= limit) return response;

			List<Match> partialMatches = partialMatches(searchword, checklist, limit - addedCount, taxonSearch, con);
			response.getPartialMatches().addAll(partialMatches);
		} catch (Exception e) {
			e.printStackTrace();
			String message = e.getMessage() == null ? "" : ": " + e.getMessage();
			response.setError(e.getClass().getSimpleName() + message);
		} finally {
			Utils.close(con);
		}
		return response;
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	private List<Match> exactMatches(String searchword, String checklist, int limit, TaxonSearch taxonSearch, TransactionConnection con) throws SQLException {
		List<Match> matches = new ArrayList<>();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = con.prepareStatement(TAXON_SEARCH_EXACT_MATCH_SQL);
			p.setString(1, searchword);
			p.setString(2, checklist);
			p.setString(3, searchword);
			rs = p.executeQuery();
			while (rs.next()) {
				Match match = toMatch(rs, taxonSearch);
				if (match == null) continue; 
				if (limit-- < 1) break;
				matches.add(match);
			}
		} finally {
			Utils.close(p, rs);
		}
		return matches;
	}

	private List<Match> likelyMatches(String searchword, String checklist, int limit, TaxonSearch taxonSearch, TransactionConnection con) throws SQLException {
		List<Match> matches = new ArrayList<>();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = con.prepareStatement(TAXON_SEARCH_LIKELY_MATCH_SQL);
			p.setString(1, searchword);
			p.setString(2, searchword);
			p.setString(3, searchword);
			p.setString(4, checklist);
			rs = p.executeQuery();
			while (rs.next()) {
				Match match = toMatch(rs, taxonSearch);
				if (match == null) continue;
				if (limit-- < 1) break;
				match.setSimilarity(rs.getDouble(3));
				matches.add(match);
			}
		} finally {
			Utils.close(p, rs);
		}
		return matches;
	}

	private List<Match> partialMatches(String searchword, String checklist, int limit, TaxonSearch taxonSearch, TransactionConnection con) throws SQLException {
		List<Match> matches = new ArrayList<>();
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = con.prepareStatement(TAXON_SEARCH_PARTIAL_MATCH_SQL);
			p.setString(1, "%" + searchword.replace(" ", "%") + "%");
			p.setString(2, searchword);
			p.setString(3, checklist);
			rs = p.executeQuery();
			while (rs.next()) {
				Match match = toMatch(rs, taxonSearch);
				if (match == null) continue;
				if (limit-- < 1) break;
				matches.add(match);
			}
		} finally {
			Utils.close(p, rs);
		}
		return matches;
	}

	private Match toMatch(ResultSet rs, TaxonSearch taxonSearch) throws SQLException {
		Qname taxonId = new Qname(rs.getString(1));
		if (!taxonContainer.hasTaxon(taxonId))  return null;
		Taxon taxon = getTaxon(taxonId);
		String name = rs.getString(2);
		Match match = new Match(taxon, name);
		if (taxonSearch.hasFilters()) {
			if (!taxonMatchesFilters(taxonSearch, taxon)) return null;
		}
		for (Qname informalGroupQname : taxon.getInformalTaxonGroups()) {
			InformalTaxonGroup informalGroup = getInformalTaxonGroups().get(informalGroupQname.toString());
			if (informalGroup == null) continue;
			match.getInformalGroups().add(informalGroup);
		}
		return match;
	}

	private boolean taxonMatchesFilters(TaxonSearch taxonSearch, Taxon taxon) {
		if (taxonSearch.isOnlySpecies()) {
			if (!taxon.isSpecies()) return false;
		}
		if (taxonSearch.isOnlyFinnish()) {
			if (!taxon.isFinnish()) return false;
		}
		if (taxonSearch.getInformalTaxonGroups().isEmpty()) return true;
		for (Qname required : taxonSearch.getInformalTaxonGroups()) {
			if (taxon.getInformalTaxonGroups().contains(required)) {
				return true;
			}
		}
		return false;
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
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Qname checklist, Qname taxonQnameToIgnore) throws Exception {
		List<Taxon> matches = new ArrayList<Taxon>();
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = triplestoreDAO.openConnection();
			if (checklist == null) {
				p = con.prepareStatement("" +
						" SELECT qname, scientificname, author, taxonrank FROM "+SCHEMA+".taxon_search_materialized " +
						" WHERE checklist IS NULL AND name = ? AND qname != ? ");
				p.setString(1, name.toUpperCase());
				p.setString(2, taxonQnameToIgnore.toString());
			} else {
				p = con.prepareStatement("" +
						" SELECT qname, scientificname, author, taxonrank FROM "+SCHEMA+".taxon_search_materialized " +
						" WHERE checklist = ? AND name = ? AND qname != ? ");
				p.setString(1, checklist.toString());
				p.setString(2, name.toUpperCase());
				p.setString(3, taxonQnameToIgnore.toString());
			}
			rs = p.executeQuery();
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

	@Override
	public EditableTaxon createTaxon() throws Exception {
		Qname qname = triplestoreDAO.getSeqNextValAndAddResource("MX");
		return new EditableTaxon(qname, taxonContainer);
	}

	@Override
	public Set<String> getInformalTaxonGroupRoots() {
		Map<String, InformalTaxonGroup> allGroups = getInformalTaxonGroups();
		Set<String> roots = new LinkedHashSet<>(allGroups.keySet());
		for (InformalTaxonGroup group : allGroups.values()) {
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
									if (area.getType().equals(BIOGEOGRAPHICAL_PROVINCE)) {
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


}
