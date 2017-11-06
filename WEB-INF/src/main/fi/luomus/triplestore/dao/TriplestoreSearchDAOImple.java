package fi.luomus.triplestore.dao;

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

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

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
	public Collection<Model> search(SearchParams searchParams) throws SQLException {
		Set<Qname> results = new HashSet<>();

		List<String> values = new ArrayList<String>();
		String sql = buildSearchSQLAndSetValues(searchParams, values);

		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try { 
			//			System.out.println(sql);
			//			System.out.println(values);
			con = dao.openConnection();
			p = con.prepareStatement(sql);
			int i = 1;
			for (String value : values) {
				p.setString(i++, value);
			}
			rs = p.executeQuery(); 
			rs.setFetchSize(4001);
			while (rs.next()) {
				results.add(new Qname(rs.getString(1)));
			}
		} finally {
			Utils.close(p, rs, con);
		}

		return get(results);
	}

	private String buildSearchSQLAndSetValues(SearchParams searchParams, List<String> values) {
		StringBuilder query = new StringBuilder();
		query.append(" SELECT  DISTINCT subjectname            \n");
		query.append(" FROM    "+SCHEMA+".rdf_statementview    \n"); 
		query.append(" WHERE   1=1                             \n");
		subjects(searchParams.getSubjects(), values, query);
		predicates(searchParams.getPredicates(), values, query);
		objects(searchParams.getObjects(), values, query);
		objectresources(searchParams.getObjectresources(), values, query);
		objectliterals(searchParams.getObjectliterals(), values, query);
		type(searchParams.getType(), values, query);
		query.append("\n ORDER BY "+SCHEMA+".GetNumericOrder(subjectname)  \n");
		query.append(" OFFSET ? ROWS FETCH FIRST ? ROWS ONLY ");
		values.add(Integer.toString(searchParams.getOffset()));
		values.add(Integer.toString(searchParams.getLimit()));
		return query.toString();
	}

	private void type(String type, List<String> values, StringBuilder query) {
		if (type == null) return;
		query
		.append(" AND subjectname IN (")
		.append("		SELECT subjectname FROM "+SCHEMA+".rdf_statementview ")
		.append("		WHERE predicatename = 'rdf:type' ");
		if (type.startsWith("http:")) {
			query.append(" AND objecturi = ? ");
		} else {
			query.append(" AND objectname = ? ");	
		}
		query.append(" ) ");
		values.add(type);
	}

	private void objectliterals(Set<String> literals, List<String> values, StringBuilder query) {
		if (literals.isEmpty()) return;
		query.append(" AND ( 1=2 ");
		for (String literal : literals) {
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

	private void objectresources(Set<String> resources, List<String> values, StringBuilder query) {
		if (resources.isEmpty()) return;
		query.append(" AND ( 1=2 ");
		for (String qname : resources) {
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

	private void objects(Set<String> resourcesOrLiterals, List<String> values, StringBuilder query) {
		if (resourcesOrLiterals.isEmpty()) return;
		query.append(" AND ( 1=2 ");
		for (String resourceOrLiteral : resourcesOrLiterals) {
			if (resourceOrLiteral.startsWith("http:")) {
				query.append(" OR objecturi = ? ");
				values.add(resourceOrLiteral);
			} else {
				if (resourceOrLiteral.contains("%")) {
					query.append(" OR objectname LIKE ? OR UPPER(resourceliteral) LIKE ? ");
					values.add(resourceOrLiteral);
					values.add(resourceOrLiteral.toUpperCase());
				} else {
					query.append(" OR objectname = ? OR resourceliteral = ? ");
					values.add(resourceOrLiteral);
					values.add(resourceOrLiteral);
				}
			}
		}
		query.append(" ) \n");
	}

	private void predicates(Set<String> predicates, List<String> values, StringBuilder query) {
		if (predicates.isEmpty()) return;
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

	private void subjects(Set<String> subjects, List<String> values, StringBuilder query) {
		if (subjects.isEmpty()) return;
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

	@Override
	public Collection<Model> get(Set<Qname> qnames, ResultType resultType) throws TooManyResultsException, Exception {
		return get(qnames, resultType, new HashSet<Qname>());
	}

	private Collection<Model> get(Set<Qname> qnames, ResultType resultType, Set<Qname> alreadyIncludedSubjects) throws TooManyResultsException, Exception {
		List<Model> models = new ArrayList<Model>();
		for (Qname qname : qnames) {
			Model model = dao.get(qname);
			if (!model.isEmpty()) {
				models.add(model);
				addResultTypeModels(resultType, models, qname, model, alreadyIncludedSubjects);
			}
		}
		return models;
	}

	private void addResultTypeModels(ResultType resultType, List<Model> models, Qname qname, Model model, Set<Qname> alreadyIncludedSubjects) throws Exception {
		if (resultType == ResultType.CHAIN) {
			models.addAll(get(fetchTaxonChain(qname)));
		} else if (resultType == ResultType.CHILDREN) {
			models.addAll(get(fetchAllChildren(qname)));
		} else if (resultType == ResultType.TREE) {
			models.addAll(fetchTree(model));
		} else if (resultType == ResultType.DEEP) {
			models.addAll(deep(model, alreadyIncludedSubjects));
		} else {
			throw new UnsupportedOperationException("Not impleted for " + resultType);
		}
	}

	private Collection<Model> deep(Model model, Set<Qname> alreadyIncludedSubjects) throws TooManyResultsException, Exception {
		if (model.getType() != null && !model.getType().equals("rdf:Alt") && !model.getType().equals("rdf:Property")) return Collections.emptyList();
		alreadyIncludedSubjects.add(new Qname(model.getSubject().getQname()));
		List<Model> models = new ArrayList<Model>();
		Set<Qname> objects = new HashSet<>();
		for (Statement s : model) {
			if (s.isResourceStatement()) {
				Qname object = new Qname(s.getObjectResource().getQname());
				if (!alreadyIncludedSubjects.contains(object)) {
					objects.add(object);
					alreadyIncludedSubjects.add(object);
				}
			}
		}
		models.addAll(get(objects, ResultType.DEEP));
		return models;
	}

	private Collection<Model> fetchTree(Model parent) throws TooManyResultsException, SQLException {
		List<Model> models = new ArrayList<Model>();
		Predicate hasPart = new Predicate("MZ.hasPart");
		Qname parentQname = qnameOf(parent);

		Set<Qname> immediateChildren = fetchImmediateChildren(parentQname);
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

	private Collection<Model> get(Set<Qname> subjects) throws SQLException {
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
			rs.setFetchSize(4001);
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

	private String constructSelectQuery(Set<Qname> results) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT  predicatename, objectname, resourceliteral, langcodefk, contextname, statementid, subjectname "); 
		sql.append(" FROM    "+SCHEMA+".rdf_statementview ");
		sql.append(" WHERE   ( subjectname IN ( ");
		int c = 0;
		Iterator<Qname> i = results.iterator();
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

	private Set<Qname> fetchTaxonChain(Qname qname) throws SQLException {
		Set<Qname> resultSet = new HashSet<>();
		Set<Qname> searchForThese = new HashSet<>();

		searchForThese.add(qname);

		TransactionConnection con = null;
		PreparedStatement p = null;
		try {
			con = dao.openConnection(); 
			p = con.prepareStatement(FETCH_TAXON_CHAIN_SQL);
			while (!searchForThese.isEmpty()) {
				Qname searchedFor = fetchTaxonChain(resultSet, searchForThese, p);
				searchForThese.remove(searchedFor);
			}
		} finally {
			Utils.close(p, con);
		}
		return resultSet;
	}

	private Qname fetchTaxonChain(Set<Qname> resultSet, Set<Qname> searchForThese, PreparedStatement p) throws SQLException {
		ResultSet rs = null;
		Qname searchForQname = searchForThese.iterator().next();
		p.setString(1, searchForQname.toString());
		try {
			rs = p.executeQuery();
			rs.setFetchSize(4001);
			while (rs.next()) {
				Qname result = new Qname(rs.getString(1));
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

	private Set<Qname> fetchAllChildren(Qname qname) throws TooManyResultsException, SQLException {
		return fetchChildren(qname, true);
	}

	private Set<Qname> fetchImmediateChildren(Qname qname) throws TooManyResultsException, SQLException {
		return fetchChildren(qname, false);
	}

	private Set<Qname> fetchChildren(Qname qname, boolean all) throws TooManyResultsException, SQLException {
		Set<Qname> resultSet = new HashSet<>();
		Set<Qname> searchForThese = new HashSet<>();

		searchForThese.add(qname);

		TransactionConnection con = null;
		PreparedStatement p = null;
		try {
			con = dao.openConnection();
			p = con.prepareStatement(FETCH_CHILDREN_SQL);
			while (!searchForThese.isEmpty()) {
				Qname searchedFor = fetchChildren(resultSet, searchForThese, p);
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

	private Qname fetchChildren(Set<Qname> resultSet, Set<Qname> searchForThese, PreparedStatement p) throws SQLException, TooManyResultsException {
		ResultSet rs = null;
		Qname searchForQname = searchForThese.iterator().next();
		p.setString(1, searchForQname.toString());
		try {
			rs = p.executeQuery();
			rs.setFetchSize(4001);
			while (rs.next()) {
				Qname result = new Qname(rs.getString(1));
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
		return search(new SearchParams().predicate(predicate).objectresource(objectresource));
	}

	@Override
	public int count(SearchParams searchParams) throws Exception {
		List<String> values = new ArrayList<String>();
		StringBuilder query = new StringBuilder();
		query.append(" SELECT  count(DISTINCT subjectname)     \n");
		query.append(" FROM    "+SCHEMA+".rdf_statementview    \n"); 
		query.append(" WHERE   1=1                             \n");
		subjects(searchParams.getSubjects(), values, query);
		predicates(searchParams.getPredicates(), values, query);
		objects(searchParams.getObjects(), values, query);
		objectresources(searchParams.getObjectresources(), values, query);
		objectliterals(searchParams.getObjectliterals(), values, query);
		type(searchParams.getType(), values, query);
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try { 
			//			System.out.println(sql);
			//			System.out.println(values);
			con = dao.openConnection();
			p = con.prepareStatement(query.toString());
			int i = 1;
			for (String value : values) {
				p.setString(i++, value);
			}
			rs = p.executeQuery(); 
			rs.next();
			return rs.getInt(1);
		} finally {
			Utils.close(p, rs, con);
		}
	}

}
