package fi.luomus.triplestore.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.RedListEvaluationGroup;
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
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.iucn.EndangermentObject;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.ResourceListing;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.models.UsedAndGivenStatements.Used;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class TriplestoreDAOImple implements TriplestoreDAO {

	private static final String MVL_INCLUDES_INFORMAL_TAXON_GROUP = "MVL.includesInformalTaxonGroup";
	private static final String MVL_INCLUDES_TAXON = "MVL.includesTaxon";
	private static final String MR_IS_PUBLIC = "MR.isPublic";
	private static final String MR_OWNER = "MR.owner";
	private static final String MR_ROOT_TAXON = "MR.rootTaxon";
	private static final String MVL_HAS_SUB_GROUP = "MVL.hasSubGroup";
	private static final String MVL_HAS_IUCN_SUB_GROUP = "MVL.hasIucnSubGroup";
	private static final String MVL_NAME = "MVL.name";
	private static final String MVL_INFORMAL_TAXON_GROUP = "MVL.informalTaxonGroup";
	private static final String MVL_IUCN_RED_LIST_TAXON_GROUP = "MVL.iucnRedListTaxonGroup";
	private static final String UNBOUNDED = "unbounded";
	private static final String MA_PERSON = "MA.person";
	private static final String RDF_LI_PREFIX = "rdf:_";
	private static final String MO_THREATENED = "MO.threatened";
	private static final String MO_NOTES = "MO.notes";
	private static final String MO_SPECIMEN_URI = "MO.specimenURI";
	private static final String MO_YEAR = "MO.year";
	private static final String MO_AREA = "MO.area";
	private static final String MO_STATUS = "MO.status";
	private static final String MO_TAXON = "MO.taxon";
	private static final String MO_OCCURRENCE = "MO.occurrence";
	private static final String RDF_ALT = "rdf:Alt";
	private static final String RDFS_LABEL = "rdfs:label";
	private static final String MZ_UNIT_OF_MEASUREMENT = "MZ.unitOfMeasurement";
	private static final String XSD_MAX_OCCURS = "xsd:maxOccurs";
	private static final String XSD_MIN_OCCURS = "xsd:minOccurs";
	private static final String SORT_ORDER = "sortOrder";
	private static final String RDFS_RANGE = "rdfs:range";
	private static final String MZ_CREATED_AT_TIMESTAMP = "MZ.createdAtTimestamp";
	private static final String MX_IS_PART_OF = "MX.isPartOf";
	private static final String MX_VERCACULAR_NAME = "MX.vernacularName";
	private static final String MX_FINNISH = "MX.finnish";
	private static final String MX_OCCURRENCE_IN_FINLAND = "MX.occurrenceInFinland";
	private static final String MX_TYPE_OF_OCCURRENCE_IN_FINLAND = "MX.typeOfOccurrenceInFinland";
	private static final String MX_NAME_ACCORDING_TO = "MX.nameAccordingTo";
	private static final String MX_TAXON_RANK = "MX.taxonRank";
	private static final String MX_SCIENTIFIC_NAME_AUTHORSHIP = "MX.scientificNameAuthorship";
	private static final String MX_SCIENTIFIC_NAME = "MX.scientificName";
	private static final String MX_NOTES = "MX.notes";
	private static final String MX_TAXON = "MX.taxon";
	private static final String RDFS_COMMENT = "rdfs:comment";
	private static final String MR_CHECKLIST = "MR.checklist";
	private static final String DC_URI = "dc:URI";
	private static final String DC_BIBLIOGRAPHIC_CITATION = "dc:bibliographicCitation";
	private static final String MP_PUBLICATION = "MP.publication";
	private static final Predicate SORT_ORDER_PREDICATE = new Predicate(SORT_ORDER);

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String DELETE_ALL_PREDICATE_CONTEXT_LANGCODE_STATEMENTS_SQL = "" +
			" DELETE from "+SCHEMA+".rdf_statementview " +
			" WHERE subjectname = ? AND predicatename = ? " + 
			" AND coalesce(contextname,'.') = ? AND coalesce(langcodefk,'.') = ? ";

	private static final String CALL_LTKM_LUONTO_ADD_STATEMENT_L = " {CALL "+SCHEMA+".AddStatementL(?, ?, ?, ?, ?, ?)} ";

	private static final String CALL_LTKM_LUONTO_ADD_STATEMENT = " {CALL "+SCHEMA+".AddStatement(?, ?, ?, ?, ?)} ";

	private static final String DELETE_FROM_RDF_STATEMENT_BY_ID_SQL = " DELETE from "+SCHEMA+".rdf_statement WHERE statementid = ? ";

	private static final String CALL_LTKM_LUONTO_ADD_RESOURCE = "{CALL "+SCHEMA+".AddResource(?)}";

	private final static String GET_MODEL_BY_QNAME_SQL = "" +
			" SELECT  predicatename, objectname, resourceliteral, langcodefk, contextname, statementid " +  
			" FROM    "+SCHEMA+".rdf_statementview                                         " + 
			" WHERE   subjectname = ?                                                      " + 
			" ORDER BY predicatename                                                       ";

	private final Qname userQname;
	private final DataSource datasource;
	private final ErrorReporter errorReporter;
	private static TriplestoreDAOImpleCaches cached;

	public TriplestoreDAOImple(DataSource datasource, Qname userQname, ErrorReporter errorReporter) {
		this.datasource = datasource;
		this.userQname = userQname;
		this.errorReporter = errorReporter;
		if (cached == null) {
			cached = new TriplestoreDAOImpleCaches(this);
		}
	}

	@Override
	public RuntimeException exception(String message, Exception e) {
		errorReporter.report(message, e);
		return new RuntimeException(message, e);
	}

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
		deleteStatement.setLong(1, s.getId());
		int i = deleteStatement.executeUpdate();
		if (i != 1) throw new IllegalStateException("Delete removed " + i + " rows instead of 1.");
	}

	@Override
	public void deleteStatement(long statementId) throws SQLException {
		TransactionConnection con = null;
		PreparedStatement deleteStatement = null;
		try {
			con = openConnection();
			con.startTransaction();
			deleteStatement = con.prepareStatement(DELETE_FROM_RDF_STATEMENT_BY_ID_SQL);
			deleteStatement.setLong(1, statementId);
			deleteStatement.executeQuery();
			con.commitTransaction();
		} finally {
			Utils.close(deleteStatement);
			Utils.close(con);
		}
	}

	private void addStatement(Statement statement, CallableStatement addStatement, Subject subject) throws SQLException {
		int i = 2;
		addStatement.setString(i++, statement.getPredicate().getQname());
		if (statement.isLiteralStatement()) {
			String content = statement.getObjectLiteral().getContent();
			if (!given(content)) return;
			addStatement.setString(i++, content);
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

	@Override
	public Checklist store(Checklist checklist) throws Exception {
		Model model = new Model(checklist.getQname());
		model.setType(MR_CHECKLIST);

		for (Map.Entry<String, String> e : checklist.getFullname().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(DC_BIBLIOGRAPHIC_CITATION, e.getValue(), e.getKey());
		}
		for (Map.Entry<String, String> e : checklist.getNotes().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(RDFS_COMMENT, e.getValue(), e.getKey());
		}
		model.addStatementIfObjectGiven(MR_ROOT_TAXON, checklist.getRootTaxon());
		model.addStatementIfObjectGiven(MR_OWNER, checklist.getOwner());
		model.addStatement(new Statement(new Predicate(MR_IS_PUBLIC), checklist.isPublic()));

		store(model);
		return checklist;
	}

	@Override
	public InformalTaxonGroup storeInformalTaxonGroup(InformalTaxonGroup group) throws Exception {
		Model model = new Model(group.getQname());
		model.setType(MVL_INFORMAL_TAXON_GROUP);
		for (Map.Entry<String, String> e : group.getName().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(MVL_NAME, e.getValue(), e.getKey());
		}
		for (Qname parent : group.getSubGroups()) {
			model.addStatementIfObjectGiven(MVL_HAS_SUB_GROUP, parent);
		}
		model.addStatement(new Statement(SORT_ORDER_PREDICATE, group.getOrder()));
		if (group.isExplicitlyDefinedRoot()) {
			model.addStatement(new Statement(new Predicate("MVL.explicitlyDefinedRoot"), true));
		}
		store(model);
		return group;
	}

	@Override
	public RedListEvaluationGroup storeIucnRedListTaxonGroup(RedListEvaluationGroup group) throws Exception {
		Model model = new Model(group.getQname());
		model.setType(MVL_IUCN_RED_LIST_TAXON_GROUP);
		for (Map.Entry<String, String> e : group.getName().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(MVL_NAME, e.getValue(), e.getKey());
		}
		for (Qname parent : group.getSubGroups()) {
			model.addStatementIfObjectGiven(MVL_HAS_IUCN_SUB_GROUP, parent);
		}
		for (Qname taxonId : group.getTaxons()) {
			model.addStatementIfObjectGiven(MVL_INCLUDES_TAXON, taxonId);
		}
		for (Qname groupId : group.getInformalGroups()) {
			model.addStatementIfObjectGiven(MVL_INCLUDES_INFORMAL_TAXON_GROUP, groupId);
		}
		model.addStatement(new Statement(SORT_ORDER_PREDICATE, group.getOrder()));
		store(model);
		return group;
	}

	@Override
	public Publication storePublication(Publication publication) throws Exception {
		if (!publication.getQname().isSet()) {
			Qname existingId = getExistingIdByCitation(publication);
			if (existingId != null) {
				publication.setQname(existingId);
			} else {
				publication.setQname(this.getSeqNextValAndAddResource("MP"));
			}
		}
		Model model = new Model(publication.getQname());
		model.setType(MP_PUBLICATION);
		model.addStatementIfObjectGiven(DC_BIBLIOGRAPHIC_CITATION, publication.getCitation(), null);
		model.addStatementIfObjectGiven(DC_URI, publication.getURI(), null);
		store(model);
		return publication;
	}

	private Qname getExistingIdByCitation(Publication publication) throws Exception {
		String citation = publication.getCitation();
		if (citation == null) return null;
		citation = ObjectLiteral.sanitizeLiteral(citation);
		Collection<Model> existing = this.getSearchDAO().search(
				new SearchParams(1, 0)
				.type(MP_PUBLICATION)
				.predicate(DC_BIBLIOGRAPHIC_CITATION)
				.objectliteral(citation));
		if (existing.isEmpty()) return null;
		return new Qname(existing.iterator().next().getSubject().getQname());
	}

	@Override
	public Taxon addTaxon(EditableTaxon taxon) throws SQLException {
		Model model = new Model(taxon.getQname());
		model.setType(MX_TAXON);

		model.addStatementIfObjectGiven(MX_SCIENTIFIC_NAME, taxon.getScientificName());
		model.addStatementIfObjectGiven(MX_SCIENTIFIC_NAME_AUTHORSHIP, taxon.getScientificNameAuthorship());
		model.addStatementIfObjectGiven(MX_TAXON_RANK, taxon.getTaxonRank());
		model.addStatementIfObjectGiven(MX_NAME_ACCORDING_TO, taxon.getChecklist());
		model.addStatementIfObjectGiven(MX_IS_PART_OF, taxon.getParentQname());
		for (Map.Entry<String, String> e : taxon.getVernacularName().getAllTexts().entrySet()) {
			model.addStatementIfObjectGiven(MX_VERCACULAR_NAME, e.getValue(), e.getKey());
		}
		if (taxon.isFinnish()) model.addStatementIfObjectGiven(MX_FINNISH, true);
		model.addStatementIfObjectGiven(MX_OCCURRENCE_IN_FINLAND, taxon.getOccurrenceInFinland());
		for (Qname typeOfOcc : taxon.getTypesOfOccurrenceInFinland()) {
			model.addStatementIfObjectGiven(MX_TYPE_OF_OCCURRENCE_IN_FINLAND, typeOfOcc);
		}
		model.addStatementIfObjectGiven(MX_NOTES, taxon.getNotes());

		String createdAt = Long.toString(DateUtils.getCurrentEpoch());
		model.addStatement(new Statement(new Predicate(MZ_CREATED_AT_TIMESTAMP), new ObjectLiteral(createdAt)));

		store(model);
		return taxon;
	}

	private boolean given(Object value) {
		return value != null && value.toString().length() > 0;
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
			statement = new Statement(new Predicate(predicatename), ObjectLiteral.unsanitazedObjectLiteral(objectliteral, langcode), context);
		} else {
			statement = new Statement(new Predicate(predicatename), new ObjectResource(objectname), context);
		}
		statement.setId(statementId);
		model.addStatement(statement);
	}

	RdfProperty createProperty(Model model) throws Exception {
		String range = getValue(RDFS_RANGE, model);
		String sortOrder = getValue(SORT_ORDER, model);
		String minOccurs = getValue(XSD_MIN_OCCURS, model);
		String maxOccurs = getValue(XSD_MAX_OCCURS, model);
		String unitOfMeasurement = getValue(MZ_UNIT_OF_MEASUREMENT, model);
		String altParent = getValue("altParent", model);
		Qname rangeQname = range == null ? null : new Qname(range); 
		RdfProperty property = new RdfProperty(new Qname(model.getSubject().getQname()), rangeQname);

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

		if (maxOccurs != null ) {
			if (UNBOUNDED.equals(maxOccurs)) {
				property.setMaxOccurs(Integer.MAX_VALUE);
			} else {
				try {
					property.setMaxOccurs(Integer.valueOf(maxOccurs));
				} catch (NumberFormatException e) {}
			}
		}

		if (unitOfMeasurement != null && range != null) {
			property.getRange().setUnitOfMeasurement(createProperty(get(unitOfMeasurement)));
		}

		if (property.hasRange() && !property.isLiteralProperty()) {
			addRangeValues(property);
		}

		LocalizedText labels = new LocalizedText();
		LocalizedText comments = new LocalizedText();
		for (Statement s : model.getStatements(RDFS_LABEL)) {
			labels.set(s.getObjectLiteral().getLangcode(), s.getObjectLiteral().getContent());
		}
		for (Statement s : model.getStatements(RDFS_COMMENT)) {
			comments.set(s.getObjectLiteral().getLangcode(), s.getObjectLiteral().getContent());
		}
		property.setLabels(labels);
		property.setComments(comments);

		if (altParent != null) {
			property.setAltParent(new Qname(altParent));
		}
		return property;
	}

	private String getValue(String string, Model model) {
		if (!model.hasStatements(string)) return null;
		Statement s = model.getStatements(string).get(0);
		if (s.isLiteralStatement()) return s.getObjectLiteral().getContent();
		return s.getObjectResource().getQname();
	}

	private void addRangeValues(RdfProperty property) throws Exception {
		if (property.getRange().getQname().toString().equals(MA_PERSON)) {
			addPersons(property);
		} else {
			Model range = get(property.getRange().getQname());
			if (RDF_ALT.equals(range.getType())) {
				property.getRange().setRangeValues(getAltValues(range));
			}
		}
	}

	private void addPersons(RdfProperty property) {
		property.getRange().setRangeValues(cached.persons.get());
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
			if (!predicate.startsWith(RDF_LI_PREFIX)) continue;
			String object = s.getObjectResource().getQname();
			Model altmodel = get(object);
			RdfProperty property = createProperty(altmodel);
			property.setOrder(Integer.valueOf(predicate.replace(RDF_LI_PREFIX, "")));
			values.add(property);
		}
		Collections.sort(values);
		return values;
	}

	@Override
	public RdfProperties getProperties(String className) {
		return cached.properties.get(className);
	}

	@Override
	public RdfProperty getProperty(Predicate predicate) {
		return cached.propery.get(predicate.getQname());
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

	@Override
	public void insert(Subject subject, Statement statement) throws SQLException {
		TransactionConnection con = null;
		CallableStatement addStatement= null;
		try {
			con = openConnection();
			con.startTransaction();

			addStatement = statement.isLiteralStatement() ? con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT_L) : con.prepareCall(CALL_LTKM_LUONTO_ADD_STATEMENT);

			addStatement.setString(1, subject.getQname());
			addStatement(statement, addStatement, subject);
			con.commitTransaction();
		} finally {
			Utils.close(addStatement);
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
	public void delete(Subject subject) throws Exception {
		store(new Model(subject));
	}

	@Override
	public void clearCaches() {
		cached.invalidateAll();
	}

	@Override
	public void store(Occurrences existingOccurrences, Occurrences alteredOccurrences, Set<Qname> supportedAreas) throws Exception {
		for (Occurrence existing : existingOccurrences) {
			if (supportedAreas.contains(existing.getArea())) {
				delete(new Subject(existing.getId()));
			}
		}
		for (Occurrence o : alteredOccurrences.getOccurrences()) {
			store(alteredOccurrences.getTaxonQname(), o);
		}
	}

	@Override
	public void store(Qname taxonQname, Occurrence occurrence) throws SQLException {
		Qname id = given(occurrence.getId()) ? occurrence.getId() : this.getSeqNextValAndAddResource("MO"); 
		Model model = new Model(id);
		model.setType(MO_OCCURRENCE);
		model.addStatementIfObjectGiven(MO_TAXON, taxonQname);
		model.addStatementIfObjectGiven(MO_STATUS, occurrence.getStatus());
		model.addStatementIfObjectGiven(MO_AREA, occurrence.getArea());
		model.addStatementIfObjectGiven(MO_YEAR, s(occurrence.getYear()));
		model.addStatementIfObjectGiven(MO_NOTES, occurrence.getNotes());
		model.addStatementIfObjectGiven(MO_THREATENED, occurrence.getThreatened());
		if (occurrence.getSpecimenURI() != null)
			model.addStatementIfObjectGiven(MO_SPECIMEN_URI, occurrence.getSpecimenURI().toString());
		this.store(model);
		occurrence.setId(id);
	}

	private String s(Integer i) {
		if (i == null) return null;
		return i.toString();
	}

	private boolean given(Qname qname) {
		return qname != null && qname.isSet();
	}

	@Override
	public TriplestoreSearchDAO getSearchDAO() {
		return new TriplestoreSearchDAOImple(this);
	}

	@Override
	public long getUserFK(String userQname) throws SQLException {
		Long id = getResourceId(userQname);
		if (id == null) throw new IllegalStateException("No user found " + userQname);
		return id;
	}

	private Long getResourceId(String resourceQname) throws SQLException {
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = openConnection();
			p = con.prepareStatement(" SELECT resourceid FROM "+SCHEMA+".rdf_resource WHERE resourcename = ? ");
			p.setString(1, resourceQname);
			rs = p.executeQuery();
			if (!rs.next()) return null;
			return rs.getLong(1);
		} finally {
			Utils.close(p, rs, con);
		}
	}



	@Override
	public List<ResourceListing> getResourceStats() {
		return cached.stats.get();
	}

	@Override
	public boolean resourceExists(String qname) throws SQLException {
		return getResourceId(qname) != null;
	}

	@Override
	public boolean resourceExists(Qname resourceQname) throws SQLException {
		return resourceExists(resourceQname.toString());
	}

	@Override
	public void store(Evaluation givenData, Evaluation existingEvaluation) throws Exception {
		storeEvaluation(givenData, existingEvaluation, false);
	}

	@Override
	public void storeOnlyOccurrences(Evaluation givenData, Evaluation existingEvaluation) throws Exception {
		storeEvaluation(givenData, existingEvaluation, true);
	}

	private void storeEvaluation(Evaluation givenData, Evaluation existingEvaluation, boolean onlyOccurrences) throws Exception {
		storeOccurrencesAndSetIdToModel(givenData);
		if (!onlyOccurrences) {
			storeEndangermentObjectsAndSetIdToModel(givenData);
			storeHabitatObjectsAndSetIdsToModel(givenData);
		}
		this.store(givenData.getModel());
		if (existingEvaluation != null) {
			deleteOccurrences(existingEvaluation);
			if (!onlyOccurrences) {
				deleteEndangermentObjects(existingEvaluation);
				deleteHabitatObjects(existingEvaluation);
			}
		}
	}

	private void storeEndangermentObjectsAndSetIdToModel(Evaluation givenData) throws Exception {
		Model model = givenData.getModel();
		for (EndangermentObject endangermentObject : givenData.getEndangermentReasons()) {
			this.store(endangermentObject);
			model.addStatement(new Statement(IucnDAO.HAS_ENDANGERMENT_REASON_PREDICATE, new ObjectResource(endangermentObject.getId())));
		}
		for (EndangermentObject endangermentObject : givenData.getThreats()) {
			this.store(endangermentObject);
			model.addStatement(new Statement(IucnDAO.HAS_THREATH_PREDICATE, new ObjectResource(endangermentObject.getId())));
		}
	}

	private void storeHabitatObjectsAndSetIdsToModel(Evaluation givenData) throws Exception {
		Model model = givenData.getModel();
		HabitatObject primaryHabitat = givenData.getPrimaryHabitat();
		if (primaryHabitat != null) {
			this.store(primaryHabitat);
			model.addStatement(new Statement(IucnDAO.PRIMARY_HABITAT_PREDICATE, new ObjectResource(primaryHabitat.getId())));
		}
		for (HabitatObject secondaryHabitat : givenData.getSecondaryHabitats()) {
			this.store(secondaryHabitat);
			model.addStatement(new Statement(IucnDAO.SECONDARY_HABITAT_PREDICATE, new ObjectResource(secondaryHabitat.getId())));
		}
	}

	private void storeOccurrencesAndSetIdToModel(Evaluation givenData) throws Exception {
		givenData.getModel().removeAll(IucnDAO.HAS_OCCURRENCE_PREDICATE);
		for (Occurrence occurrence : givenData.getOccurrences()) {
			this.store(new Qname(givenData.getSpeciesQname()), occurrence);
			givenData.getModel().addStatement(new Statement(IucnDAO.HAS_OCCURRENCE_PREDICATE, new ObjectResource(occurrence.getId())));
		}
	}

	private void deleteHabitatObjects(Evaluation existingEvaluation) throws Exception {
		if (existingEvaluation.getPrimaryHabitat() != null) {
			this.delete(new Subject(existingEvaluation.getPrimaryHabitat().getId()));
		}
		for (HabitatObject habitat : existingEvaluation.getSecondaryHabitats()) {
			this.delete(new Subject(habitat.getId()));
		}
	}

	private void deleteOccurrences(Evaluation existingEvaluation) throws Exception {
		for (Occurrence occurrence : existingEvaluation.getOccurrences()) {
			this.delete(new Subject(occurrence.getId()));
		}
	}

	private void deleteEndangermentObjects(Evaluation existingEvaluation) throws Exception {
		for (EndangermentObject endangermentObject : existingEvaluation.getEndangermentReasons()) {
			this.delete(new Subject(endangermentObject.getId()));
		}
		for (EndangermentObject endangermentObject : existingEvaluation.getThreats()) {
			this.delete(new Subject(endangermentObject.getId()));
		}
	}

	@Override
	public void store(HabitatObject habitat) throws Exception {
		Qname id = given(habitat.getId()) ? habitat.getId() : this.getSeqNextValAndAddResource(Evaluation.IUCN_EVALUATION_NAMESPACE);
		habitat.setId(id);
		Model model = new Model(new Subject(id));
		model.setType(Evaluation.HABITAT_OBJECT_CLASS);
		model.addStatement(new Statement(IucnDAO.HABITAT_PREDICATE, new ObjectResource(habitat.getHabitat())));
		for (Qname type : habitat.getHabitatSpecificTypes()) {
			model.addStatement(new Statement(IucnDAO.HABITAT_SPESIFIC_TYPE_PREDICATE, new ObjectResource(type)));
		}
		model.addStatement(new Statement(SORT_ORDER_PREDICATE, new ObjectLiteral(String.valueOf(habitat.getOrder()))));
		this.store(model);
	}

	private void store(EndangermentObject endangermentObject) throws Exception {
		Qname id = given(endangermentObject.getId()) ? endangermentObject.getId() : getSeqNextValAndAddResource(Evaluation.IUCN_EVALUATION_NAMESPACE);
		endangermentObject.setId(id);
		Model model = new Model(id);
		model.setType(Evaluation.ENDANGERMENT_OBJECT_CLASS);
		model.addStatementIfObjectGiven(Evaluation.ENDANGERMENT, endangermentObject.getEndangerment());
		model.addStatementIfObjectGiven(SORT_ORDER, String.valueOf(endangermentObject.getOrder()));
		this.store(model);
	}

	public String getSchema() {
		return SCHEMA;
	}

	@Override
	public Map<String, List<RdfProperty>> getDescriptionGroupVariables() {
		return cached.descriptionGroupVariables.get();
	}

	@Override
	public List<RdfProperty> getDescriptionGroups() {
		return cached.descriptionGroups.get();
	}

}
