package fi.luomus.triplestore.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.db.connectivity.SimpleTransactionConnection;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.Cached.ResourceWrapper;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.ResourceListing;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.models.UsedAndGivenStatements.Used;

public class TriplestoreDAOImple implements TriplestoreDAO {

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;
	private final Qname userQname;
	private final DataSource datasource;

	public TriplestoreDAOImple(DataSource datasource, Qname userQname) {
		this.datasource = datasource;
		this.userQname = userQname;
	}

	private static final String DELETE_ALL_PREDICATE_CONTEXT_LANGCODE_STATEMENTS_SQL = "" +
			" DELETE from "+SCHEMA+".rdf_statementview " +
			" WHERE subjectname = ? AND predicatename = ? " + 
			" AND coalesce(contextname,'.') = ? AND coalesce(langcodefk,'.') = ? ";

	private static final String PERSON_NEXTVAL_SQL = "" + 
			" SELECT max(to_number(substr(qname, 4))) 			" + 
			" FROM ( 											" +  
			" 	SELECT DISTINCT subjectname AS qname 			" + 
			"	FROM "+SCHEMA+".rdf_statementview 				" + 
			"	WHERE predicatename = 'rdf:type' 				" + 
			"	AND objectname = 'MA.person' 	 				" +
			" )													" +	
			" WHERE to_number(substr(qname, 4)) < 10000 		";

	private static final String CALL_LTKM_LUONTO_ADD_STATEMENT_L = " {CALL "+SCHEMA+".AddStatementL(?, ?, ?, ?, ?, ?)} ";

	private static final String CALL_LTKM_LUONTO_ADD_STATEMENT = " {CALL "+SCHEMA+".AddStatement(?, ?, ?, ?, ?)} ";

	private static final String DELETE_FROM_RDF_STATEMENT_BY_ID_SQL = " DELETE from "+SCHEMA+".rdf_statement WHERE statementid = ? ";

	private static final String CALL_LTKM_LUONTO_ADD_RESOURCE = "{CALL "+SCHEMA+".AddResource(?)}";

	private final static String GET_MODEL_BY_QNAME_SQL = "" +
			" SELECT  predicatename, objectname, resourceliteral, langcodefk, contextname, statementid " +  
			" FROM    "+SCHEMA+".rdf_statementview                                         " + 
			" WHERE   subjectname = ?                                                      " + 
			" ORDER BY predicatename                                                       ";

	private final static String GET_PROPERTIES_BY_CLASSNAME_SQL = "" + 
			" SELECT DISTINCT 													" +
			" 		propertyName, 												" + 
			" 		ranges.objectname AS range,									" +
			"		sortOrder.resourceliteral AS sortOrder,						" +
			"		min.resourceLiteral AS minOccurs,							" + 
			"		max.resourceLiteral As maxOccurs 							" + 
			" FROM																" +
			" ((																" +
			" 	 SELECT DISTINCT v.predicatename AS propertyName				" +
			" 	 FROM "+SCHEMA+".rdf_statementview v 							" +
			"    WHERE v.subjectname IN ( 										" +																				
			" 	   SELECT DISTINCT subjectname FROM "+SCHEMA+".rdf_statementview WHERE predicatename = 'rdf:type' AND objectname = ?		" + 				
			" 	 ) 																" +
			" ) UNION (															" +
			"   SELECT DISTINCT subjectname as propertyName						" +
			"   FROM "+SCHEMA+".rdf_statementview 								" +
			"   WHERE predicatename = 'rdfs:domain' AND objectname = ?			" +
			" )) properties														" +
			" LEFT JOIN "+SCHEMA+".rdf_statementview ranges ON (ranges.subjectname = propertyName AND ranges.predicatename = 'rdfs:range')	" +
			" LEFT JOIN "+SCHEMA+".rdf_statementview sortOrder ON (sortOrder.subjectname = propertyName AND sortOrder.predicatename = 'sortOrder')	" +
			" LEFT JOIN "+SCHEMA+".rdf_statementview min on (min.subjectname = propertyName AND min.predicatename = 'xsd:minOccurs') " + 
			" LEFT JOIN "+SCHEMA+".rdf_statementview max on (max.subjectname = propertyName AND max.predicatename = 'xsd:maxOccurs') ";

	private final static String GET_PROPERTY_BY_PREDICATE_NAME_SQL = "" + 
			" SELECT ranges.objectname AS range, sortOrder.resourceliteral as sortOrder " +
			" FROM "+SCHEMA+".rdf_statementview ranges " +
			" LEFT JOIN "+SCHEMA+".rdf_statementview sortOrder ON (sortOrder.subjectname = ranges.subjectname AND sortOrder.predicatename = 'sortOrder')	" + 
			" WHERE ranges.subjectname = ? AND ranges.predicatename = 'rdfs:range' ";

	@Override
	public TransactionConnection openConnection() throws SQLException {
		return new SimpleTransactionConnection(datasource.getConnection());
	}

	@Override
	public Qname addResource(Qname qname) throws SQLException {
		TransactionConnection con = null;
		try {
			con = openConnection();
			addResource(qname, con);
		} finally {
			Utils.close(con);
		}
		return qname;
	}

	private void addResource(Qname qname, TransactionConnection con) throws SQLException {
		CallableStatement stmt = null;
		try {
			con.startTransaction();
			stmt = con.prepareCall(CALL_LTKM_LUONTO_ADD_RESOURCE);
			stmt.setString(1, qname.toString());
			stmt.executeUpdate();
			con.commitTransaction();
		} finally {
			Utils.close(stmt);
		}
	}

	@Override
	public void store(Model model) throws SQLException {
		TransactionConnection con = null;
		try {
			con = openConnection();
			con.startTransaction();
			store(model, con);
			con.commitTransaction();
		} finally {
			Utils.close(con);
		}
	}

	private void store(Model model, TransactionConnection con) throws SQLException {
		Subject subject =  model.getSubject();
		Model existingModel = this.get(subject);

		PreparedStatement deleteStatement = null;
		CallableStatement addResourceStatement = null;
		CallableStatement addLiteralStatement = null;
		try {
			deleteStatement = con.prepareStatement(DELETE_FROM_RDF_STATEMENT_BY_ID_SQL);
			addResourceStatement = con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT);
			addLiteralStatement = con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT_L);

			addResourceStatement.setString(1, subject.getQname());
			addLiteralStatement.setString(1, subject.getQname());

			for (Statement s : model) {
				if (!existingModel.hasStatement(s)) {
					if (s.isLiteralStatement()) {
						addStatement(s, addLiteralStatement, subject);
					} else {
						addStatement(s, addResourceStatement, subject);
					}
				}
			}
			for (Statement s : existingModel) {
				if (!model.hasStatement(s)) {
					deleteStatement(s, deleteStatement);
				}
			}
		} finally {
			Utils.close(deleteStatement);
			Utils.close(addResourceStatement);
			Utils.close(addLiteralStatement);
		}
	}

	private void deleteStatement(Statement s, PreparedStatement deleteStatement) throws SQLException {
		deleteStatement.setInt(1, s.getId());
		int i = deleteStatement.executeUpdate();
		if (i != 1) throw new IllegalStateException("Delete removed " + i + " rows instead of 1.");
	}

	private void addStatement(Statement statement, CallableStatement addStatement, Subject subject) throws SQLException {
		int i = 2;
		addStatement.setString(i++, statement.getPredicate().getQname());
		if (statement.isLiteralStatement()) {
			String content = statement.getObjectLiteral().getContent();
			if (!given(content)) return;
			if (content.length() >= 4000) {
				throw new IllegalArgumentException("Content is longer than 4000 characters.");
			}
			addStatement.setString(i++, statement.getObjectLiteral().getContent());
			addStatement.setString(i++, statement.getObjectLiteral().getLangcode());
		} else {
			addStatement.setString(i++, statement.getObjectResource().getQname());
		}
		addStatement.setString(i++, userQname == null ? null : userQname.toString());
		addStatement.setString(i++, statement.isForDefaultContext() ? null : statement.getContext().getQname());
		try {
			int c = addStatement.executeUpdate();
			if (c != 1) throw new IllegalStateException("Add statement inserted " + c + " rows instead of 1.");
		} catch (SQLException sqle) {
			if (sqle.getMessage() != null && sqle.getMessage().startsWith("ORA-01403")) {
				throw reportMissingResource(statement, subject, sqle);
			}
		}
	}

	private RuntimeException reportMissingResource(Statement statement, Subject subject, SQLException sqle) throws SQLException {
		if (!resourceExists(subject.getQname())) {
			return new IllegalArgumentException("Subject '" + subject.getQname() + "' does not exist.");
		}
		if (!resourceExists(statement.getPredicate().getQname())) {
			return new IllegalArgumentException("Predicate '" + statement.getPredicate().getQname() + "' does not exist.");
		}
		if (statement.isResourceStatement() && !resourceExists(statement.getObjectResource().getQname())) {
			return new IllegalArgumentException("Object '" + statement.getObjectResource().getQname() + "' does not exist for predicate: " + statement.getPredicate().getQname());
		}
		if (!statement.isForDefaultContext() && !resourceExists(statement.getContext().getQname())) {
			return new IllegalArgumentException("Context '" + statement.getContext().getQname() + "' does not exist for predicate: " + statement.getPredicate().getQname());
		}
		return new IllegalArgumentException(sqle);
	}

	@Override
	public Qname getSeqNextValAndAddResource(String qnamePrefix) throws SQLException {
		if (qnamePrefix.equalsIgnoreCase("MA")) {
			Qname personQname = personNextVal();
			addResource(personQname);
			return personQname;
		}
		String query = " SELECT "+SCHEMA+".rdf_"+qnamePrefix.toLowerCase()+"_seq.nextval FROM dual ";
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
			p = con.prepareStatement(query);
			rs = p.executeQuery();
			rs.next();
			int nextval = rs.getInt(1);
			Qname qname = new Qname(qnamePrefix.toUpperCase() + "." + nextval);
			addResource(qname, con);
			return qname;
		} finally {
			Utils.close(p, rs, con);
		}
	}

	private Qname personNextVal() throws SQLException {
		String sql = PERSON_NEXTVAL_SQL;
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
			p = con.prepareStatement(sql);
			rs = p.executeQuery();
			rs.next();
			int max = rs.getInt(1);
			int nextval = max + 1;
			return new Qname("MA." + nextval);
		} finally {
			Utils.close(p, rs, con);
		}
	}

	@Override
	public Checklist store(Checklist checklist) throws Exception {
		Model model = new Model(checklist.getQname());
		model.setType("MR.checklist");

		for (Map.Entry<String, String> e : checklist.getFullname().getAllTexts().entrySet()) {
			model.addStamentIfObjectGiven("dc:bibliographicCitation", e.getValue(), e.getKey());
		}
		for (Map.Entry<String, String> e : checklist.getNotes().getAllTexts().entrySet()) {
			model.addStamentIfObjectGiven("rdfs:comment", e.getValue(), e.getKey());
		}
		model.addStatementIfObjectGiven("MR.rootTaxon", checklist.getRootTaxon());
		model.addStatementIfObjectGiven("MR.owner", checklist.getOwner());
		model.addStatement(new Statement(new Predicate("MR.isPublic"), checklist.isPublic()));

		store(model);
		return checklist;
	}

	@Override
	public InformalTaxonGroup storeInformalTaxonGroup(InformalTaxonGroup group) throws Exception {
		Model model = new Model(group.getQname());
		model.setType("MVL.informalTaxonGroup");
		for (Map.Entry<String, String> e : group.getName().getAllTexts().entrySet()) {
			model.addStamentIfObjectGiven("MVL.name", e.getValue(), e.getKey());
		}
		for (Qname parent : group.getSubGroups()) {
			model.addStatementIfObjectGiven("MVL.hasSubGroup", parent);
		}
		store(model);
		return group;
	}

	@Override
	public Publication storePublication(Publication publication) throws Exception {
		Model model = new Model(publication.getQname());
		model.setType("MP.publication");
		model.addStamentIfObjectGiven("dc:bibliographicCitation", publication.getCitation(), null);
		model.addStamentIfObjectGiven("dc:URI", publication.getURI(), null);
		store(model);
		return publication;
	}

	@Override
	public Taxon addTaxon(Taxon taxon) throws SQLException {
		Model model = new Model(taxon.getQname());
		model.setType("MX.taxon");

		model.addStatementIfObjectGiven("MX.scientificName", taxon.getScientificName());
		model.addStatementIfObjectGiven("MX.scientificNameAuthorship", taxon.getScientificNameAuthorship());
		model.addStatementIfObjectGiven("MX.taxonRank", taxon.getTaxonRank());

		model.addStatementIfObjectGiven("MX.nameAccordingTo", taxon.getChecklist());
		model.addStatementIfObjectGiven("MX.isPartOf", taxon.getParentQname());

		if (!given(taxon.getTaxonConcept())) {
			taxon.setTaxonConcept(this.addTaxonConcept());
		}
		model.addStatement(new Statement(new Predicate("MX.circumscription"), new ObjectResource(taxon.getTaxonConcept())));

		String createdAt = Long.toString(DateUtils.getCurrentEpoch());
		model.addStatement(new Statement(new Predicate("MZ.createdAtTimestamp"), new ObjectLiteral(createdAt)));

		store(model);
		return taxon;
	}

	private boolean given(Object value) {
		return value != null && value.toString().length() > 0;
	}

	@Override
	public Qname addTaxonConcept() throws SQLException {
		return getSeqNextValAndAddResource("MC");
	}

	@Override
	public void store(Subject subject, Statement statement) throws Exception {
		UsedAndGivenStatements usedAndGivenStatements = new UsedAndGivenStatements();
		usedAndGivenStatements.addStatement(statement);
		store(subject, usedAndGivenStatements);
	}

	public Model get(Subject subject) throws SQLException {
		return get(subject.getQname());
	}

	@Override
	public Model get(Qname qname) throws SQLException {
		return get(qname.toString());
	}

	@Override
	public Model get(String qname) throws SQLException {
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
			p = con.prepareStatement(GET_MODEL_BY_QNAME_SQL);
			p.setString(1, qname);
			rs = p.executeQuery();
			return toModel(rs, qname);
		} finally {
			Utils.close(p, rs, con);
		}
	}

	private Model toModel(ResultSet rs, String qname) throws SQLException {
		Model model = new Model(new Subject(qname));
		while (rs.next()) {
			toModel(rs, model);
		}
		return model;
	}

	public void toModel(ResultSet rs, Model model) throws SQLException {
		String predicatename = rs.getString(1);
		String objectname = rs.getString(2);
		String objectliteral = rs.getString(3);
		String contextname = rs.getString(5);
		int statementId = rs.getInt(6);
		Context context = contextname == null ? null : new Context(contextname);
		Statement statement = null;
		if (objectliteral != null) {
			String langcode = rs.getString(4);
			statement = new Statement(new Predicate(predicatename), new ObjectLiteral(objectliteral, langcode), context);
		} else {
			statement = new Statement(new Predicate(predicatename), new ObjectResource(objectname), context);
		}
		statement.setId(statementId);
		model.addStatement(statement);
	}



	private static final Cached<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperties> PROPERTIES_CACHE = new Cached<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperties>(new PropertiesCacheLoader(), 60*60, 500);

	private static class PropertiesCacheLoader implements CacheLoader<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperties> {

		@Override
		public RdfProperties load(ResourceWrapper<String, TriplestoreDAOImple> wrapper) {
			String className = wrapper.getKey();
			TriplestoreDAOImple dao = wrapper.getResource();
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				con = dao.openConnection();
				p = con.prepareStatement(GET_PROPERTIES_BY_CLASSNAME_SQL);
				p.setString(1, className);
				p.setString(2, className);
				rs = p.executeQuery();
				RdfProperties properties = new RdfProperties();
				while (rs.next()) {
					Qname predicate = new Qname(rs.getString(1));
					Qname range = rs.getString(2) == null ? null : new Qname(rs.getString(2));
					String sortOrder = rs.getString(3);
					String minOccurs = rs.getString(4);
					String maxOccurs = rs.getString(5);
					RdfProperty property = dao.createProperty(predicate, range);
					if (sortOrder != null) {
						try {
							property.setOrder(Integer.valueOf(sortOrder));
						} catch (NumberFormatException e) {}
					}
					if (minOccurs != null) {
						try {
							property.setMinOccurs(Integer.valueOf(minOccurs));
						} catch (NumberFormatException e) {}
					}
					if (maxOccurs != null) {
						if ("unbounded".equals(maxOccurs)) {
							property.setMaxOccurs(Integer.MAX_VALUE);
						} else {
							try {
								property.setMaxOccurs(Integer.valueOf(maxOccurs));
							} catch (NumberFormatException e) {}
						}
					}
					properties.addProperty(property);
				}
				return properties;
			} catch (Exception e) {
				throw new RuntimeException("Properties cache loader for classname " + className + ". " + e.getMessage(), e);
			}
			finally {
				Utils.close(p, rs, con);
			}
		}
	}

	private RdfProperty createProperty(Qname propertyQname, Qname range) throws Exception {
		RdfProperty property = new RdfProperty(propertyQname, range);

		if (property.hasRange() && !property.isLiteralProperty()) {
			addRangeValues(property);
		}

		addLabels(property);
		return property;
	}

	private void addLabels(RdfProperty property) throws SQLException {
		LocalizedText localizedText = new LocalizedText();
		Model model = get(property.getQname());
		for (Statement s : model.getStatements("rdfs:label")) {
			localizedText.set(s.getObjectLiteral().getLangcode(), s.getObjectLiteral().getContent());
		}
		property.setLabels(localizedText);
	}

	private void addRangeValues(RdfProperty property) throws Exception {
		if (property.getRange().getQname().toString().equals("MA.person")) {
			addPersons(property);
			return;
		}
		String rangeType = getTypeOTheRange(property.getRange().getQname());
		if ("rdf:Alt".equals(rangeType)) {
			addRangeValuesForAlt(property);
		}
	}

	private String getTypeOTheRange(Qname rangeQname) throws SQLException {
		Model model = get(rangeQname);
		return model.getType();
	}

	private static final SingleObjectCacheResourceInjected<List<RdfProperty>, TriplestoreDAO> CACHED_PERSONS = new SingleObjectCacheResourceInjected<>(new SingleObjectCacheResourceInjected.CacheLoader<List<RdfProperty>, TriplestoreDAO>() {
		@Override
		public List<RdfProperty> load(TriplestoreDAO dao) {
			try {
				List<RdfProperty> rangeValues = new ArrayList<RdfProperty>();
				for (Model m : dao.getSearchDAO().search("rdf:type", "MA.person")) {
					RdfProperty rangeValue = new RdfProperty(new Qname(m.getSubject().getQname()), null);
					String personName = m.getSubject().getQname();
					for (Statement s : m.getStatements("MA.fullName")) {
						personName = s.getObjectLiteral().getContent();
						break;
					}
					rangeValue.setLabels(new LocalizedText().set("fi", personName).set("en", personName));
					rangeValues.add(rangeValue);
				}
				return rangeValues;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}, 60*15);

	private void addPersons(RdfProperty property) throws Exception {
		property.getRange().setRangeValues(CACHED_PERSONS.get(this));
	}

	private void addRangeValuesForAlt(RdfProperty property) throws Exception {
		List<RdfProperty> values = getAltValues(property.getRange().getQname());
		property.getRange().setRangeValues(values);
	}

	@Override
	public List<RdfProperty> getAltValues(Qname qname) throws Exception {
		Model model = get(qname);
		List<RdfProperty> values = getAltValues(model);
		return values;
	}

	private List<RdfProperty> getAltValues(Model model) throws Exception {
		List<RdfProperty> values = new ArrayList<>();
		for (Statement s : model.getStatements()) {
			if (s.isLiteralStatement()) continue;
			String predicate = s.getPredicate().getQname();
			if (!predicate.startsWith("rdf:_")) continue;
			String object = s.getObjectResource().getQname();
			RdfProperty property = createProperty(new Qname(object), null);
			property.setOrder(Integer.valueOf(predicate.replace("rdf:_", "")));
			values.add(property);
		}
		Collections.sort(values);
		return values;
	}

	@Override
	public RdfProperties getProperties(String className) throws SQLException {
		return PROPERTIES_CACHE.get(new ResourceWrapper<String, TriplestoreDAOImple>(className, this));
	}

	private static final Cached<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperty> SINGLE_PROPETY_CACHE = new Cached<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperty>(new SinglePropertyCacheLoader(), 60*60, 500);

	private static class SinglePropertyCacheLoader implements CacheLoader<ResourceWrapper<String, TriplestoreDAOImple>, RdfProperty> {

		@Override
		public RdfProperty load(ResourceWrapper<String, TriplestoreDAOImple> key) {
			String predicateQname = key.getKey();
			TriplestoreDAOImple dao = key.getResource();
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				con = dao.openConnection();
				p = con.prepareStatement(GET_PROPERTY_BY_PREDICATE_NAME_SQL);
				p.setString(1, predicateQname);
				rs = p.executeQuery();
				while (rs.next()) {
					Qname range = rs.getString(1) == null ? null : new Qname(rs.getString(1));
					String sortOrder = rs.getString(2);
					RdfProperty property = dao.createProperty(new Qname(predicateQname), range);
					if (sortOrder != null) {
						try {
							property.setOrder(Integer.valueOf(sortOrder));
						} catch (NumberFormatException e) {}
					}
					return property;
				}
				return new RdfProperty(new Qname(predicateQname), null);
			} catch (Exception e) {
				throw new RuntimeException("Single property cache loader for predicate " + predicateQname + ". " + e.getMessage());
			} finally {
				Utils.close(p, rs, con);
			}
		}

	}
	@Override
	public RdfProperty getProperty(Predicate predicate) throws Exception {
		return SINGLE_PROPETY_CACHE.get(new ResourceWrapper<String, TriplestoreDAOImple>(predicate.getQname(), this));
	}

	@Override
	public void delete(Subject subject, Predicate predicate) throws SQLException {
		delete(subject, predicate, null);
	}

	@Override
	public void delete(Subject subject, Predicate predicate, Context context) throws SQLException {
		UsedAndGivenStatements used = new UsedAndGivenStatements();
		used.addUsed(predicate, context, null);
		store(subject, used);
	}

	@Override
	public void store(Subject subject, UsedAndGivenStatements usedAndGivenStatements) throws SQLException {
		TransactionConnection con = null;
		PreparedStatement removePredicatesStatement = null;
		CallableStatement addStatement= null;
		CallableStatement addStatementL = null;
		try {
			con = openConnection();
			con.startTransaction();

			removePredicatesStatement = con.prepareStatement(DELETE_ALL_PREDICATE_CONTEXT_LANGCODE_STATEMENTS_SQL);
			addStatement = con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT);
			addStatementL = con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT_L);

			removePredicatesStatement.setString(1, subject.getQname());
			addStatement.setString(1, subject.getQname());
			addStatementL.setString(1, subject.getQname());

			for (Used used : usedAndGivenStatements.getUsed()) {
				removeStatements(used.getPredicate(), used.getContext(), used.getLangcode(), removePredicatesStatement);
			}
			for (Statement givenStatement : usedAndGivenStatements.getGivenStatements()) {
				if (givenStatement.isLiteralStatement()) {
					addStatement(givenStatement, addStatementL, subject);
				} else {
					addStatement(givenStatement, addStatement, subject);
				}
			}

			con.commitTransaction();
		} finally {
			Utils.close(removePredicatesStatement);
			Utils.close(addStatement);
			Utils.close(addStatementL);
			Utils.close(con);
		}
	}


	private void removeStatements(Predicate predicate, Context context, String langCode, PreparedStatement removePredicatesStatement) throws SQLException {
		removePredicatesStatement.setString(2, predicate.getQname());
		if (context == null || !given(context.getQname())) {
			removePredicatesStatement.setString(3, ".");
		} else {
			removePredicatesStatement.setString(3, context.getQname());
		}
		if (!given(langCode)) {
			removePredicatesStatement.setString(4, ".");
		} else {
			removePredicatesStatement.setString(4, langCode);
		}
		removePredicatesStatement.executeUpdate();
	}

	@Override
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Qname checklist, Qname taxonQnameToIgnore) throws Exception { // TODO miksei tämä ole taxonomydaossa?
		List<Taxon> matches = new ArrayList<Taxon>();
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
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
				Taxon match = new Taxon(matchQname, null, null, null);
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
	public void delete(Subject subject) throws Exception {
		store(new Model(subject));
	}

	@Override
	public void clearCaches() {
		PROPERTIES_CACHE.invalidateAll();
		SINGLE_PROPETY_CACHE.invalidateAll();
		CACHED_PERSONS.invalidate();
		CACHED_RESOURCE_STATS.invalidate();
	}

	@Override
	public void store(Occurrences existingOccurrences, Occurrences alteredOccurrences) throws Exception {
		// TODO voiko nyt käyttää jonkin muun toteutuksen logiikkaa?  Voi: store(model), mutta tämä hakee jokaisen occurences kannasta erikseen ja tämä on jo tehtynä -- ok?
		for (Occurrence o : alteredOccurrences.getOccurrences()) {
			Qname area = o.getArea();
			Occurrence existing = existingOccurrences.getOccurrence(area);
			if (existing == null) {
				// insert
				store(alteredOccurrences.getTaxonQname(), o);
			} else if (!existing.getStatus().equals(o.getStatus())) {
				// update
				existing.setStatus(o.getStatus());
				updateOccurrence(existing);
			}
		}
		for (Occurrence o : existingOccurrences.getOccurrences()) {
			Qname area = o.getArea();
			if (alteredOccurrences.getOccurrence(area) == null) {
				// delete
				this.delete(new Subject(o.getId()));
			}
		}
	}

	private void updateOccurrence(Occurrence o) throws SQLException, Exception {
		this.store(new Subject(o.getId()), new Statement(new Predicate("MO.status"), new ObjectResource(o.getStatus())));
	}
	
	@Override
	public void store(Qname taxonQname, Occurrence occurrence) throws SQLException {
		Qname id = given(occurrence.getId()) ? this.getSeqNextValAndAddResource("MO") : occurrence.getId(); 
		Model model = new Model(id);
		model.setType("MO.occurrence");
		model.addStatementIfObjectGiven("MO.taxon", taxonQname);
		model.addStatementIfObjectGiven("MO.status", occurrence.getStatus());
		model.addStatementIfObjectGiven("MO.area", occurrence.getArea());
		this.store(model);
		occurrence.setId(id);
	}

	private boolean given(Qname qname) {
		return qname != null && qname.isSet();
	}

	@Override
	public TriplestoreSearchDAO getSearchDAO() {
		return new TriplestoreSearchDAOImple(this);
	}

	@Override
	public int getUserFK(String userQname) throws SQLException {
		Integer id = getResourceId(userQname);
		if (id == null) throw new IllegalStateException("No user found " + userQname);
		return id;
	}

	private Integer getResourceId(String resourceQname) throws SQLException {
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
			p = con.prepareStatement(" SELECT resourceid FROM "+SCHEMA+".rdf_resource WHERE resourcename = ? ");
			p.setString(1, resourceQname);
			rs = p.executeQuery();
			if (!rs.next()) return null;
			return rs.getInt(1);
		} finally {
			Utils.close(p, rs, con);
		}
	}

	private static final SingleObjectCacheResourceInjected<List<ResourceListing>, TriplestoreDAO> CACHED_RESOURCE_STATS = new SingleObjectCacheResourceInjected<List<ResourceListing>, TriplestoreDAO>(new ResourceStatCacheLoader(), 60*15);

	private static class ResourceStatCacheLoader implements SingleObjectCacheResourceInjected.CacheLoader<List<ResourceListing>, TriplestoreDAO> {
		@Override
		public List<ResourceListing> load(TriplestoreDAO usingDAO) {
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				con = usingDAO.openConnection();
				p = con.prepareStatement(" SELECT resourcename, resourcecount FROM " + SCHEMA + ".rdf_classview_materialized ORDER BY resourcecount DESC ");
				rs = p.executeQuery();
				List<ResourceListing> listing = new ArrayList<ResourceListing>();
				while (rs.next()) {
					listing.add(new ResourceListing(rs.getString(1), rs.getInt(2)));
				}
				return listing;
			} catch (Exception e) {
				e.printStackTrace();
				return Collections.emptyList();
			} finally {
				Utils.close(p, rs, con);
			}
		}
	};

	@Override
	public List<ResourceListing> getResourceStats() throws Exception {
		return CACHED_RESOURCE_STATS.get(this);
	}

	@Override
	public boolean resourceExists(String qname) throws SQLException {
		return getResourceId(qname) != null;
	}

	@Override
	public boolean resourceExists(Qname resourceQname) throws SQLException {
		return resourceExists(resourceQname.toString());
	}

}
