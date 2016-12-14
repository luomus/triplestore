package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAOBaseImple;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class ExtendedTaxonomyDAOImple extends TaxonomyDAOBaseImple implements ExtendedTaxonomyDAO {

	private static final double JARO_WINKLER_DISTANCE = 0.88;
	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String TAXON_SEARCH_LIKELY_MATCH_SQL = "" +
			" SELECT   qname, name, scientificname, author, taxonrank, utl_match.jaro_winkler(name, ?) match " +
			" FROM     "+SCHEMA+".taxon_search_materialized   " +
			" WHERE    utl_match.jaro_winkler(name, ?) > " + JARO_WINKLER_DISTANCE +
			" AND      name != ? " + 
			" AND      COALESCE(checklist, '.') = ? " + 
			" ORDER BY match DESC, name  ";

	private static final String TAXON_SEARCH_PARTIAL_MATCH_SQL = "" +
			" SELECT   qname, name, scientificname, author, taxonrank  " +
			" FROM     "+SCHEMA+".taxon_search_materialized   " +
			" WHERE    name LIKE ? " +
			" AND      name != ? " +
			" AND      COALESCE(checklist, '.') = ? " + 
			" ORDER BY name  ";

	private static final String TAXON_SEARCH_EXACT_MATCH_SQL = "" +
			" SELECT   qname, name, scientificname, author, taxonrank            " +
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
			Collection<Model> models = triplestoreDAO.getSearchDAO().search("MO.taxon", taxon.getQname().toString());
			for (Model model : models) {
				System.out.println("Found occurrence " + model);
				Qname id = q(model.getSubject());
				Qname area = null;
				Qname status = null;
				for (Statement s : model.getStatements()) {
					if (s.getPredicate().getQname().equals("MO.area")) {
						area = q(s.getObjectResource());
					}
					if (s.getPredicate().getQname().equals("MO.status")) {
						status = q(s.getObjectResource());
					}
				}
				taxon.getOccurrences().setOccurrence(id, area, status);
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
		Document results = initResults();
		if (!given(taxonSearch.getSearchword())) {
			results.getRootNode().addAttribute("error", "Search word must be given.");
			return results;
		}
		String searchword = taxonSearch.getSearchword().trim().toUpperCase();
		if (searchword.trim().length() <= 1) {
			results.getRootNode().addAttribute("error", "Search word was too short.");
			return results;
		}
		String checklist = ".";
		if (taxonSearch.hasChecklist()) {
			checklist = taxonSearch.getChecklist().toString().trim().toUpperCase();
		}
		int limit = taxonSearch.getLimit();
		int addedCount = 0;
		Set<Qname> requiredInformalTaxonGroups = taxonSearch.getInformalTaxonGroups();
		TransactionConnection con = null;
		try {
			con = triplestoreDAO.openConnection();

			Node exactMatch = exactMatch(searchword, checklist, limit, requiredInformalTaxonGroups, con);
			addedCount = addIfHasMatches(exactMatch, results);
			if (addedCount >= limit) return results;

			//if (!exactMatch.hasChildNodes() && searchword.length() > 3) {
			if (searchword.length() > 3) {
				Node likelyMatches = likelyMatches(searchword, checklist, limit - addedCount, requiredInformalTaxonGroups, con);
				addedCount += addIfHasMatches(likelyMatches, results);
				if (addedCount >= limit) return results;

				Node partialMatches = partialMatches(searchword, checklist, limit - addedCount, requiredInformalTaxonGroups, con);
				addIfHasMatches(partialMatches, results);
			}
		} catch (Exception e) {
			e.printStackTrace();
			String message = e.getMessage() == null ? "" : ": " + e.getMessage();
			results.getRootNode().addAttribute("error", e.getClass().getSimpleName() + message);
		} finally {
			Utils.close(con);
		}
		return results;
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	private Node exactMatch(String searchword, String checklist, int limit, Set<Qname> requiredInformalTaxonGroups, TransactionConnection con) throws SQLException {
		Node exactMatch = new Node("exactMatch");
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = con.prepareStatement(TAXON_SEARCH_EXACT_MATCH_SQL);
			p.setString(1, searchword);
			p.setString(2, checklist);
			p.setString(3, searchword);
			rs = p.executeQuery();
			while (rs.next()) {
				Node match = toMatch(rs, requiredInformalTaxonGroups);
				if (match == null) continue; 
				if (limit-- < 1) break;
				exactMatch.addChildNode(match);
			}
		} finally {
			Utils.close(p, rs);
		}
		return exactMatch;
	}

	private Node likelyMatches(String searchword, String checklist, int limit, Set<Qname> requiredInformalTaxonGroups, TransactionConnection con) throws SQLException {
		Node likelyMatches = new Node("likelyMatches");
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
				Node match = toMatch(rs, requiredInformalTaxonGroups);
				if (match == null) continue;
				if (limit-- < 1) break;
				Double similarity = Utils.round(rs.getDouble(6), 3);
				likelyMatches.addChildNode(match).addAttribute("similarity", similarity.toString());
			}
		} finally {
			Utils.close(p, rs);
		}
		return likelyMatches;
	}

	private Node partialMatches(String searchword, String checklist, int limit, Set<Qname> requiredInformalTaxonGroups, TransactionConnection con) throws SQLException {
		Node partialMatches = new Node("partialMatches");
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = con.prepareStatement(TAXON_SEARCH_PARTIAL_MATCH_SQL);
			p.setString(1, "%" + searchword.replace(" ", "%") + "%");
			p.setString(2, searchword);
			p.setString(3, checklist);
			rs = p.executeQuery();
			while (rs.next()) {
				Node match = toMatch(rs, requiredInformalTaxonGroups);
				if (match == null) continue;
				if (limit-- < 1) break;
				partialMatches.addChildNode(match);
			}
		} finally {
			Utils.close(p, rs);
		}
		return partialMatches;
	}

	private Node toMatch(ResultSet rs, Set<Qname> requiredInformalTaxonGroups) throws SQLException {
		String qname = rs.getString(1);
		String name = rs.getString(2);
		String scientificName = rs.getString(3);
		String author = rs.getString(4);
		String taxonrank = rs.getString(5);
		Node match = new Node(qname);
		match.addAttribute("matchingName", name.toLowerCase());
		if (given(scientificName)) {
			match.addAttribute("scientificName", scientificName);
		}
		if (given(author)) {
			match.addAttribute("scientificNameAuthorship", author);
		}
		if (given(taxonrank)) {
			match.addAttribute("taxonRank", taxonrank);
		}
		Qname taxonId = new Qname(qname);
		if (taxonContainer.hasTaxon(taxonId)) {
			return handleTaxonMatch(requiredInformalTaxonGroups, match, taxonId);
		}
		if (required(requiredInformalTaxonGroups)) return null;
		return match;
	}

	private Node handleTaxonMatch(Set<Qname> requiredInformalTaxonGroups, Node match, Qname taxonId) {
		Taxon taxon = getTaxon(taxonId);
		Set<Qname> taxonsInformalGroups = taxon.getInformalTaxonGroups();
		if (required(requiredInformalTaxonGroups)) {
			boolean matches = taxonMatchesOneRequiredGroup(requiredInformalTaxonGroups, taxonsInformalGroups);
			if (!matches) return null;
		}
		addInformalGroupsToMatch(match, taxonsInformalGroups);
		return match;
	}

	private boolean required(Set<Qname> requiredInformalTaxonGroups) {
		return !requiredInformalTaxonGroups.isEmpty();
	}

	private void addInformalGroupsToMatch(Node match, Set<Qname> taxonsInformalGroups) {
		Node informalGroupsNode = match.addChildNode("informalGroups");
		for (Qname informalGroupQname : taxonsInformalGroups) {
			InformalTaxonGroup informalGroup = getInformalTaxonGroups().get(informalGroupQname.toString());
			if (informalGroup == null) continue;
			Node informalGroupNode = informalGroupsNode.addChildNode(informalGroup.getQname().toString());
			for (Map.Entry<String, String> e : informalGroup.getName().getAllTexts().entrySet()) {
				informalGroupNode.addAttribute(e.getKey(), e.getValue());
			}
		}
	}

	private boolean taxonMatchesOneRequiredGroup(Set<Qname> requiredInformalTaxonGroups, Set<Qname> taxonsInformalGroups) {
		boolean found = false;
		for (Qname required : requiredInformalTaxonGroups) {
			if (taxonsInformalGroups.contains(required)) {
				found = true;
			}
		}
		return found;
	}

	private int addIfHasMatches(Node matches, Document results) {
		if (matches.hasChildNodes()) {
			results.getRootNode().addChildNode(matches);
			return matches.getChildNodes().size();
		}
		return 0;
	}

	private static Document initResults() {
		Document results = new Document("results");
		return results;
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

}
