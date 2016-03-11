package fi.luomus.triplestore.dao;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TriplestoreSearchDAOImple implements TriplestoreSearchDAO {

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String FETCH_CHILDREN_SQL = "" + 
			" SELECT DISTINCT subjectname                      " + 
			" FROM "+SCHEMA+".rdf_statementview               " + 
			" WHERE objectname = ?                             " + 
			" AND ( predicatename IN ( 						   " + 
			" 		SELECT DISTINCT subjectname FROM "+SCHEMA+".rdf_statementview              " + 
			"       WHERE predicatename = 'rdfs:subPropertyOf' AND objectname = 'MZ.isPartOf' ) " + 
			" 		OR predicatename = 'MZ.isPartOf' ) ";

	private static final String FETCH_TAXON_CHAIN_SQL = "" + 
			" SELECT objectname                   " + 
			" FROM "+SCHEMA+".rdf_statementview  " + 
			" WHERE subjectname = ?               " + 
			" AND predicatename = 'MX.isPartOf'   ";

	private final TriplestoreDAOImple dao;

	public TriplestoreSearchDAOImple(TriplestoreDAOImple dao) {
		this.dao = dao;
	}

	@Override
	public Collection<Model> search(String[] subjects, String[] predicates, String[] objects, String[] objectresources, String[] objectliterals, int limit, int offset) throws SQLException {
		Set<String> results = new HashSet<String>();

		List<String> values = new ArrayList<String>();
		String sql = buildSearchSQLAndSetValues(subjects, predicates, objects, objectresources, objectliterals, limit, offset, values);

		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try { 
			// System.out.println(sql);
			// System.out.println(values);
			con = dao.openConnection();
			p = con.prepareStatement(sql);
			int i = 1;
			for (String value : values) {
				p.setString(i++, value);
			}
			rs = p.executeQuery(); 
			while (rs.next()) {
				results.add(rs.getString(1));
			}
		} finally {
			Utils.close(p, rs, con);
		}

		return get(results);
	}

	private String buildSearchSQLAndSetValues(String[] subjects, String[] predicates, String[] objects, String[] objectresources, String[] objectliterals, int limit, int offset, List<String> values) {
		StringBuilder query = new StringBuilder();
		query.append(" SELECT  subjectname FROM (                        \n");
		query.append(" SELECT  DISTINCT subjectname,                     \n");
		query.append("         row_number() OVER (ORDER BY "+SCHEMA+".GetNumericOrder(subjectname), subjectname) roworder  \n");
		query.append(" FROM    "+SCHEMA+".rdf_statementview    \n"); 
		query.append(" WHERE   1=1                              \n");
		subjects(subjects, values, query);
		predicates(predicates, values, query);
		objects(objects, values, query);
		objectresources(objectresources, values, query);
		objectliterals(objectliterals, values, query);
		query.append(" ) \n");
		query.append(" WHERE roworder BETWEEN ? and ? \n");
		query.append(" ORDER BY roworder ");

		values.add(Integer.toString(offset + 1));
		values.add(Integer.toString(limit + offset));
		return query.toString();
	}

	private void objectliterals(String[] objectliterals, List<String> values, StringBuilder query) {
		if (objectliterals != null) {
			query.append(" AND ( 1=2 ");
			for (String literal : objectliterals) {
				if (literal.contains("%")) {
					query.append(" OR UPPER(resourceliteral) LIKE ? ");
					values.add(literal.toUpperCase());
				} else {
					query.append(" OR resourceliteral = ? ");
					values.add(literal);
				}
			}
			query.append(" ) \n");
		}
	}

	private void objectresources(String[] objectresources, List<String> values, StringBuilder query) {
		if (objectresources != null) {
			query.append(" AND ( 1=2 ");
			for (String qname : objectresources) {
				if (qname.contains("%")) {
					query.append(" OR UPPER(objectname) LIKE ? ");
					values.add(qname.toUpperCase());
				} else {
					query.append(" OR objectname = ? ");
					values.add(qname);
				}
			}
			query.append(" ) \n");
		}
	}

	private void objects(String[] objects, List<String> values, StringBuilder query) {
		if (objects != null) {
			query.append(" AND ( 1=2 ");
			for (String object : objects) {
				if (object.startsWith("http:")) {
					query.append(" OR objecturi = ? ");
					values.add(object);
				} else {
					if (object.contains("%")) {
						query.append(" OR objectname LIKE ? OR UPPER(resourceliteral) LIKE ? ");
						values.add(object);
						values.add(object.toUpperCase());
					} else {
						query.append(" OR objectname = ? OR resourceliteral = ? ");
						values.add(object);
						values.add(object);
					}
				}
			}
			query.append(" ) \n");
		}
	}

	private void predicates(String[] predicates, List<String> values, StringBuilder query) {
		if (predicates != null) {
			query.append(" AND ( 1=2 ");
			for (String predicate : predicates) {
				if (predicate.startsWith("http:")) {
					query.append(" OR predicateuri = ? ");
				} else {
					query.append(" OR predicatename = ? ");
				}
				values.add(predicate);
			}
			query.append(" ) \n");
		}
	}

	private void subjects(String[] subjects, List<String> values, StringBuilder query) {
		if (subjects != null) {
			query.append(" AND ( 1=2 ");
			for (String subject : subjects) {
				if (subject.startsWith("http:")) {
					query.append(" OR subjecturi = ? ");
				} else {
					query.append(" OR subjectname like ? ");	
				}
				values.add(subject);
			}
			query.append(" ) \n");
		}
	}

	@Override
	public Collection<Model> get(List<Qname> qnames, ResultType resultType) throws TooManyResultsException, Exception {
		List<Model> models = new ArrayList<Model>();
		for (Qname qname : qnames) {
			Model model = dao.get(qname);
			if (!model.isEmpty()) {
				models.add(model);
				addResultTypeModels(resultType, models, qname, model);
			}
		}
		return models;
	}

	private void addResultTypeModels(ResultType resultType, List<Model> models, Qname qname, Model model) throws SQLException, TooManyResultsException {
		Set<String> subjectsToInclude = Collections.emptySet();

		if (resultType == ResultType.CHAIN) {
			subjectsToInclude = fetchTaxonChain(qname);
		} else if (resultType == ResultType.CHILDREN) {
			subjectsToInclude = fetchAllChildren(qname);
		} else if (resultType == ResultType.TREE) {
			models.addAll(fetchTree(model));
		} else {
			throw new UnsupportedOperationException("Not impleted for " + resultType);
		}

		if (!subjectsToInclude.isEmpty()) {
			models.addAll(get(subjectsToInclude));
		}
	}

	private Collection<Model> fetchTree(Model parent) throws TooManyResultsException, SQLException {
		List<Model> models = new ArrayList<Model>();
		Predicate hasPart = new Predicate("MZ.hasPart");
		Qname parentQname = qnameOf(parent);

		Set<String> immediateChildren = fetchImmediateChildren(parentQname);
		if (immediateChildren.isEmpty()) return models;

		Collection<Model> immediateChildModels = get(immediateChildren);
		models.addAll(immediateChildModels);

		for (Model child : immediateChildModels) {
			parent.addStatement(new Statement(hasPart, new ObjectResource(child.getSubject().getQname())));
			removeReverseLink(parentQname, child);
			models.addAll(fetchTree(child));
		}

		return models;
	}

	private void removeReverseLink(Qname parentQname, Model child) {
		Iterator<Statement> i = child.iterator();
		while (i.hasNext()) {
			Statement s = i.next();
			if (s.isLiteralStatement()) continue;
			if (s.getObjectResource().getQname().equals(parentQname.toString())) {
				i.remove();
			}
		}
	}

	private Qname qnameOf(Model parent) {
		return new Qname(parent.getSubject().getQname());
	}

	private Collection<Model> get(Set<String> subjects) throws SQLException {
		List<Model> models = new ArrayList<Model>();
		if (subjects.isEmpty()) return models;

		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = dao.openConnection();
			String sql = constructSelectQuery(subjects);
			p = con.prepareStatement(sql);
			rs = p.executeQuery();
			String prevSubject = null;
			Model currentModel = null;
			while (rs.next()) {
				String subject = rs.getString(7);
				if (!subject.equals(prevSubject)) {
					if (currentModel != null) {
						models.add(currentModel);
					}
					currentModel = new Model(new Qname(subject));
					prevSubject = subject;
				}
				dao.toModel(rs, currentModel);
			}
			if (currentModel != null) {
				models.add(currentModel);
			}
		} finally {
			Utils.close(p, rs, con);
		}
		return models;
	}

	private String constructSelectQuery(Set<String> results) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT  predicatename, objectname, resourceliteral, langcodefk, contextname, statementid, subjectname "); 
		sql.append(" FROM    "+SCHEMA+".rdf_statementview ");
		sql.append(" WHERE   ( subjectname IN ( ");
		int c = 0;
		Iterator<String> i = results.iterator();
		while (i.hasNext()) {
			sql.append("'").append(i.next()).append("'");
			if (i.hasNext()) {
				if (c++ > 990) {
					c = 0;
					sql.append(" ) OR subjectname IN ( ");
				} else {
					sql.append(", ");				
				}	
			}
		}
		sql.append(" ) ) ");
		sql.append(" ORDER BY subjecturi, predicateuri ");
		return sql.toString();
	}

	private Set<String> fetchTaxonChain(Qname qname) throws SQLException {
		Set<String> resultSet = new HashSet<String>();
		Set<String> searchForThese = new HashSet<String>();

		searchForThese.add(qname.toString());

		TransactionConnection con = null;
		PreparedStatement p = null;
		try {
			con = dao.openConnection(); 
			p = con.prepareStatement(FETCH_TAXON_CHAIN_SQL);
			while (!searchForThese.isEmpty()) {
				String searchedFor = fetchTaxonChain(resultSet, searchForThese, p);
				searchForThese.remove(searchedFor);
			}
		} finally {
			Utils.close(p, con);
		}
		return resultSet;
	}

	private String fetchTaxonChain(Set<String> resultSet, Set<String> searchForThese, PreparedStatement p) throws SQLException {
		ResultSet rs = null;
		String searchForQname = searchForThese.iterator().next();
		p.setString(1, searchForQname);
		try {
			rs = p.executeQuery();
			while (rs.next()) {
				String result = rs.getString(1);
				if (resultSet.contains(result)) {
					throw new IllegalStateException("Taxon loop! " + result + " " + searchForQname);
				}
				resultSet.add(result);
				searchForThese.add(result);
			}
		} finally {
			if (rs != null) rs.close();
		}
		return searchForQname;
	}

	private Set<String> fetchAllChildren(Qname qname) throws TooManyResultsException, SQLException {
		return fetchChildren(qname, true);
	}

	private Set<String> fetchImmediateChildren(Qname qname) throws TooManyResultsException, SQLException {
		return fetchChildren(qname, false);
	}

	private Set<String> fetchChildren(Qname qname, boolean all) throws TooManyResultsException, SQLException {
		Set<String> resultSet = new HashSet<String>();
		Set<String> searchForThese = new HashSet<String>();

		searchForThese.add(qname.toString());

		TransactionConnection con = null;
		PreparedStatement p = null;
		try {
			con = dao.openConnection();
			p = con.prepareStatement(FETCH_CHILDREN_SQL);
			while (!searchForThese.isEmpty()) {
				String searchedFor = fetchChildren(resultSet, searchForThese, p);
				searchForThese.remove(searchedFor);
				if (!all) {
					searchForThese.clear();
				}
			}
		} finally {
			Utils.close(p, con);
		}
		return resultSet;
	}

	private String fetchChildren(Set<String> resultSet, Set<String> searchForThese, PreparedStatement p) throws SQLException, TooManyResultsException {
		ResultSet rs = null;
		String searchForQname = searchForThese.iterator().next();
		p.setString(1, searchForQname);
		try {
			rs = p.executeQuery();
			while (rs.next()) {
				String result = rs.getString(1);
				if (resultSet.contains(result)) {
					throw new IllegalStateException("Child loop! " + result + " " + searchForQname);
				}
				resultSet.add(result);
				if (resultSet.size() >= 1000) {
					throw new TooManyResultsException();
				}
				searchForThese.add(result);
			}
		} finally {
			if (rs != null) rs.close();
		}
		return searchForQname;
	}

	@Override
	public Collection<Model> search(String predicate, String objectresource) throws Exception {
		return search(null, new String[]{predicate}, null, new String[]{objectresource}, null, 1000, 0);
	}

}
