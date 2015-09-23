package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAOBaseImple;
import fi.luomus.commons.taxonomy.TripletToTaxonHandler;
import fi.luomus.commons.taxonomy.TripletToTaxonHandlers;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.Model;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtendedTaxonomyDAOImple extends TaxonomyDAOBaseImple implements ExtendedTaxonomyDAO {

	private static final String MASTER_CHECKLIST_QNAME = "MR.1";
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

	private final TripletToTaxonHandlers tripletToTaxonHandlers = new TripletToTaxonHandlers();
	private final Cached<Qname, EditableTaxon> cachedTaxons = new Cached<Qname, EditableTaxon>(new TaxonLoader(), 1*60*60, 5000);
	private final Cached<Qname, Set<Qname>> cachedChildren = new Cached<Qname, Set<Qname>>(new ChildrenLoader(), 1*60*60, 5000);
	private final Cached<Qname, Set<Qname>> cachedSynonyms = new Cached<Qname, Set<Qname>>(new SynonymLoader(), 1*60*60, 5000);
	private final TriplestoreDAO triplestoreDAO;

	public ExtendedTaxonomyDAOImple(Config config, TriplestoreDAO triplestoreDAO) {
		super(config, 60 * 5, 20);
		this.triplestoreDAO = triplestoreDAO;
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		cachedTaxons.invalidateAll();
		cachedChildren.invalidateAll();
		cachedSynonyms.invalidateAll();
	}

	private class TaxonLoader implements CacheLoader<Qname, EditableTaxon> {
		@Override
		public EditableTaxon load(Qname qname) {
			try {
				Model model = triplestoreDAO.get(qname);
				return createTaxon(model);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class ChildrenLoader implements CacheLoader<Qname, Set<Qname>> {
		@Override
		public Set<Qname> load(Qname taxonQname) {
			try {
				Set<Qname> childTaxons = new HashSet<Qname>();
				Collection<Model> models = triplestoreDAO.getSearchDAO().search("MX.isPartOf", taxonQname.toString());
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

	private class SynonymLoader implements CacheLoader<Qname, Set<Qname>> {
		@Override
		public Set<Qname> load(Qname taxonQname) {
			try {
				Taxon taxon = getTaxon(taxonQname);
				Set<Qname> synonyms = new HashSet<Qname>();
				Collection<Model> models = triplestoreDAO.getSearchDAO().search("MX.circumscription", taxon.getTaxonConcept().toString());
				for (Model model : models) {
					EditableTaxon synonymTaxon = createTaxon(model);
					cachedTaxons.put(synonymTaxon.getQname(), synonymTaxon);
					if (!synonymTaxon.getQname().equals(taxon.getQname())) {
						synonyms.add(synonymTaxon.getQname());
					}
				}
				return synonyms;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public EditableTaxon getTaxon(Qname qname) {
		return cachedTaxons.get(qname);
	}

	public EditableTaxon createTaxon(Model model) {
		Qname qname = q(model.getSubject());
		EditableTaxon taxon = new EditableTaxon(qname, this);
		for (Statement statement : model.getStatements()) {
			if ("MX.isPartOf".equals(statement.getPredicate().getQname())) {
				Qname parentQname = q(statement.getObjectResource());
				taxon.setParentQname(parentQname);
			} else {
				addPropertyToTaxon(taxon, statement);
			}
		}
		return taxon;
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

	@Override
	public Collection<Taxon> getChildTaxons(Taxon taxon) {
		List<Taxon> childTaxons = new ArrayList<Taxon>();
		for (Qname childQname : cachedChildren.get(taxon.getQname())) {
			childTaxons.add(getTaxon(childQname));
		}
		Collections.sort(childTaxons);
		return childTaxons;
	}

	@Override
	public Collection<Taxon> getSynonymTaxons(Taxon taxon) {
		if (taxon.getTaxonConcept() == null) return Collections.emptyList();

		List<Taxon> synonymTaxons = new ArrayList<Taxon>();
		Set<Qname> synonyms = cachedSynonyms.get(taxon.getQname());
		for (Qname synonym : synonyms) {
			synonymTaxons.add(cachedTaxons.get(synonym));
		}
		return synonymTaxons;
	}

	@Override
	public void invalidateTaxon(Qname qname) {
		if (qname == null) return;
		Taxon taxon = getTaxon(qname);
		invalidateTaxon(taxon);		
	}

	@Override
	public void invalidateTaxon(Taxon taxon) {
		if (taxon == null) return;
		cachedTaxons.invalidate(taxon.getQname());
		cachedChildren.invalidate(taxon.getQname());
		cachedSynonyms.invalidate(taxon.getQname());		
	}

	@Override
	public void addOccurrences(EditableTaxon taxon) {
		try {
			Collection<Model> models = triplestoreDAO.getSearchDAO().search("MO.taxon", taxon.getQname().toString());
			for (Model model : models) {
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

	@Override
	public Document search(String searchword) throws Exception{
		return search(searchword, MASTER_CHECKLIST_QNAME);
	}

	@Override
	public Document search(String searchword, String checklist) throws Exception {
		Document results = initResults();
		if (!given(searchword)) {
			results.getRootNode().addAttribute("error", "Search word must be given.");
			return results;
		}
		searchword = searchword.trim().toUpperCase();
		if (searchword.trim().length() <= 1) {
			results.getRootNode().addAttribute("error", "Search word was too short.");
			return results;
		}
		if (checklist != null) {
			checklist = checklist.trim().toUpperCase();
		} else {
			checklist = ".";
		}

		TransactionConnection con = null;
		try {
			con = triplestoreDAO.openConnection();
			Node exactMatch = exactMatch(searchword, checklist, con);
			addIfHasMatches(exactMatch, results);
			if (!exactMatch.hasChildNodes() && searchword.length() > 3) {
				Node likelyMatches = likelyMatches(searchword, checklist, con);
				addIfHasMatches(likelyMatches, results);
				Node partialMatches = partialMatches(searchword, checklist, con);
				addIfHasMatches(partialMatches, results);
			}
		} catch (Exception e) {
			results.getRootNode().addAttribute("error", e.getMessage());
		} finally {
			Utils.close(con);
		}
		return results;
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	private Node exactMatch(String searchword, String checklist, TransactionConnection con) throws SQLException {
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
				exactMatch.addChildNode(toMatch(rs));
			}
		} finally {
			Utils.close(p, rs);
		}
		return exactMatch;
	}

	private Node likelyMatches(String searchword, String checklist, TransactionConnection con) throws SQLException {
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
				Double similarity = Utils.round(rs.getDouble(6), 3);
				likelyMatches.addChildNode(toMatch(rs).addAttribute("similarity", similarity.toString()));
			}
		} finally {
			Utils.close(p, rs);
		}
		return likelyMatches;
	}

	private Node partialMatches(String searchword, String checklist, TransactionConnection con) throws SQLException {
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
				partialMatches.addChildNode(toMatch(rs));
			}
		} finally {
			Utils.close(p, rs);
		}
		return partialMatches;
	}

	private Node toMatch(ResultSet rs) throws SQLException {
		String qname = rs.getString(1);
		String name = rs.getString(2);
		String scientificName = rs.getString(3);
		String author = rs.getString(4);
		String taxonrank = rs.getString(5);
		Node match = new Node(qname).setContents(name.toLowerCase());
		if (given(scientificName)) {
			match.addAttribute("scientificName", scientificName);
		}
		if (given(author)) {
			match.addAttribute("scientificNameAuthorship", author);
		}
		if (given(taxonrank)) {
			match.addAttribute("taxonRank", taxonrank);
		}
		return match;
	}

	private void addIfHasMatches(Node matches, Document results) {
		if (matches.hasChildNodes()) {
			results.getRootNode().addChildNode(matches);
		}
	}

	private static Document initResults() {
		Document results = new Document("results");
		return results;
	}

}
