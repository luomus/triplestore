package fi.luomus.triplestore.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.triplestore.models.ResourceListing;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public interface TriplestoreDAO {

	public static enum ResultType { NORMAL, CHAIN, CHILDREN, TREE, DEEP }

	Qname SYSTEM_USER = null;
	Qname TEST_USER = new Qname("MA.5"); // Esko Piirainen

	/**
	 * Increases sequence of qnamePrefix (for example "MA") by one, adds the resource, for example "MA.234" and returns the qname ("MA.234")
	 * @param qnamePrefix
	 * @return the new qname.
	 * @throws Exception
	 */
	Qname getSeqNextValAndAddResource(String qnamePrefix) throws Exception;

	/**
	 * Adds or modifies a checklist. For new checklist the ID must be already set.
	 * @param checklist
	 * @return the same checklist.
	 * @throws Exception
	 */
	Checklist store(Checklist checklist) throws Exception;

	/**
	 * Adds or modifies an informal taxon group. For new groups the ID must be already set.
	 * @param group
	 * @return the same group.
	 * @throws Exception
	 */
	InformalTaxonGroup storeInformalTaxonGroup(InformalTaxonGroup group) throws Exception;

	/**
	 * Adds or modifies an IUCN red list informal taxon group. For new groups the ID must be already set.
	 * @param group
	 * @return the same group
	 * @throws Exception
	 */
	RedListEvaluationGroup storeIucnRedListTaxonGroup(RedListEvaluationGroup group) throws Exception;

	/**
	 * Add a taxon into db. Taxon must have Qname already set.
	 * @param taxon
	 * @return the same taxon
	 * @throws Exception
	 */
	Taxon addTaxon(EditableTaxon taxon) throws Exception;

	/**
	 * Removes all existing [subject, predicate, context]-objects from the db and adds this new statement.
	 * If the statement already exists, nothing is done to that statement (but other [subject, predicate, context] -objects will be removed).
	 * Do not use this operation for models that can have multiple [subject, predicate, context]-objects and you don't want to remove them all.
	 * @param statement
	 * @throws Exception
	 */
	void store(Subject subject, Statement statement) throws Exception;

	/**
	 * Will insert this new statement without deleting existing [subject, predicate, context]-objects
	 * @param subject
	 * @param statement
	 * @throws Exception
	 */
	void insert(Subject subject, Statement statement) throws Exception;

	/**
	 * Reads information of all properties that are used in contect of the given class (rdf:type == className)
	 * @param className
	 * @return
	 * @throws Exception
	 */
	RdfProperties getProperties(String className) throws Exception;

	/**
	 * Reads information of this property
	 * @param predicate
	 * @return
	 * @throws Exception
	 */
	RdfProperty getProperty(Predicate predicate) throws Exception;

	/**
	 * Reads rdf:_li values of rdf:Alt
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	List<RdfProperty> getAltValues(Qname qname) throws Exception;

	/**
	 * Return model by qname
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	Model get(String qname) throws Exception;

	/**
	 * Return model by qname
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	Model get(Qname qname) throws Exception;

	/**
	 * Adds model to db or updates the db to match the given model. Those statements of the model that already exist in the db are left unchanged.
	 * @param model
	 * @throws Exception
	 */
	void store(Model model) throws Exception;

	/**
	 * Deletes all predicates of the given resource
	 * @param subject
	 * @throws Exception
	 */
	void delete(Subject subject) throws Exception;

	/**
	 * Delete one statement by id - will not fail if statement does not exist
	 * @param id
	 * @throws Exception
	 */
	void deleteStatement(long id) throws Exception;

	/**
	 * Deletes single predicates of the given resource from default context with no language
	 * @param subject
	 * @param predicate
	 * @throws SQLException
	 */
	void delete(Subject subject, Predicate predicate) throws SQLException;

	/**
	 * Deletes single predicates of the given resource from the given context with no language
	 * @param subject
	 * @param predicate
	 * @param context null for default context
	 * @throws SQLException
	 */
	void delete(Subject subject, Predicate predicate, Context context) throws SQLException;

	/**
	 * Removes used predicates from db and adds given statements.
	 * @param usedAndGivenStatements
	 * @throws SQLException
	 */
	void store(Subject subject, UsedAndGivenStatements usedAndGivenStatements) throws Exception;

	/**
	 * Adds or modifies a publiction. For new publication the ID must be already set.
	 * @param publication
	 * @return the same publication
	 * @throws Exception
	 */
	Publication storePublication(Publication publication) throws Exception;

	/**
	 * Clears for example Properties cache
	 */
	void clearCaches();

	/**
	 * Updates occurrence information, making the neccessary deletions, updates and inserts.
	 * @param existingOccurrences
	 * @param newOccurrences
	 * @param supportedAreas all existing occurrences for these areas are deleted
	 * @throws Exception
	 */
	void store(Occurrences existingOccurrences, Occurrences newOccurrences, Set<Qname> supportedAreas) throws Exception;

	/**
	 * Insert or update occurrence. ID of occurrence may or may not be set for insert; if not given it will be generated and set to object.
	 * @param taxonQname
	 * @param occurrence
	 * @throws Exception
	 */
	void store(Qname taxonQname, Occurrence occurrence) throws Exception;

	/**
	 * Get search dao
	 * @return
	 */
	TriplestoreSearchDAO getSearchDAO();

	/**
	 * Get user resourceid
	 * @param userQname
	 * @return
	 * @throws Exception
	 */
	long getUserFK(String userQname) throws Exception;

	/**
	 * Opens connection that should be closed.
	 * @return
	 * @throws Exception
	 */
	TransactionConnection openConnection() throws Exception;

	/**
	 * Adds a resource. Has no effect if resource already exists.
	 * @param qname
	 * @throws SQLException
	 */
	Qname addResource(Qname qname) throws Exception;

	List<ResourceListing> getResourceStats() throws Exception;

	/**
	 * Checks if resource exists.
	 * @param qname
	 * @return
	 * @throws SQLException
	 */
	boolean resourceExists(String qname) throws Exception;

	/**
	 * Checks if resource exists.
	 * @param resourceQname
	 * @return
	 * @throws SQLException
	 */
	boolean resourceExists(Qname resourceQname) throws Exception;

	/**
	 * Store IUCN evaluation
	 * @param givenData
	 * @param existingEvaluation
	 * @throws Exception
	 */
	void store(Evaluation givenData, Evaluation existingEvaluation) throws Exception;

	/**
	 * Store IUCN evaluation occurrences
	 * @param givenData
	 * @param existingEvaluation
	 * @throws Exception
	 */
	void storeOnlyOccurrences(Evaluation givenData, Evaluation existingEvaluation) throws Exception;

	/**
	 * Store habitat object
	 * @param habitat
	 * @throws Exception
	 */
	void store(HabitatObject habitat) throws Exception;

	/**
	 * Report and throw
	 * @param message
	 * @param e
	 * @return runtime exception that wraps the original exception, with the same message
	 */
	RuntimeException exception(String message, Exception e);

	/**
	 * Get list of properties of description groups. Keys are qnames of the groups.
	 * @return
	 */
	Map<String, List<RdfProperty>> getDescriptionGroupVariables();

	/**
	 * Get ids, names of description groups
	 * @return
	 */
	List<RdfProperty> getDescriptionGroups();

	/**
	 * Remove unused publications
	 * @return number of removed
	 * @throws SQLException
	 */
	int removeUnusedPublications() throws Exception;

	/**
	 * Check model does not contain characters that can not be parsed with Jena
	 * @param model
	 * @throws RDFValidationException
	 */
	void validate(Model model) throws RDFValidationException;

}
