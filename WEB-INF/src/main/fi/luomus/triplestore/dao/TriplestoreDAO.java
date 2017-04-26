package fi.luomus.triplestore.dao;

import java.sql.SQLException;
import java.util.List;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.Publication;
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
import fi.luomus.triplestore.models.ResourceListing;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public interface TriplestoreDAO {

	public static enum ResultType { NORMAL, CHAIN, CHILDREN, TREE, DEEP }

	public static final Qname SYSTEM_USER = null;
	public static final Qname TEST_USER = new Qname("MA.5"); // Esko Piirainen

	/**
	 * Increases sequence of qnamePrefix (for example "MA") by one, adds the resource, for example "MA.234" and returns the qname ("MA.234")
	 * @param qnamePrefix
	 * @return the new qname.
	 * @throws Exception
	 */
	public Qname getSeqNextValAndAddResource(String qnamePrefix) throws Exception;

	/**
	 * Adds or modifies a checklist. For new checklist the ID must be already set.
	 * @param checklist
	 * @return the same checklist.
	 * @throws Exception
	 */
	public Checklist store(Checklist checklist) throws Exception;

	/**
	 * Adds or modifies an informal taxon group. For new groups the ID must be already set.
	 * @param group
	 * @return the same group.
	 * @throws Exception
	 */
	public InformalTaxonGroup storeInformalTaxonGroup(InformalTaxonGroup group) throws Exception;

	/**
	 * Add a taxon into db. Taxon must have Qname already set. If taxon doesn't have a taxon concept, a new one is generated.
	 * @param taxon
	 * @return the same taxon with possibly a newly created taxon concept.
	 * @throws Exception
	 */
	public Taxon addTaxon(EditableTaxon taxon) throws Exception;

	/**
	 * Removes all existing [subject, predicate, context]-objects from the db and adds this new statement. 
	 * If the statement already exists, nothing is done to that statement (but other [subject, predicate, context] -objects will be removed).
	 * Do not use this operation for models that can have multiple [subject, predicate, context]-objects and you don't want to remove them all.
	 * @param statement
	 * @throws Exception
	 */
	public void store(Subject subject, Statement statement) throws Exception;

	/**
	 * Will insert this new statement without deleting existing [subject, predicate, context]-objects
	 * @param subject
	 * @param statement
	 * @throws Exception
	 */
	public void insert(Subject subject, Statement statement) throws Exception;
	
	/**
	 * Reads information of all properties that are used in contect of the given class (rdf:type == className)
	 * @param className
	 * @return
	 * @throws Exception 
	 */
	public RdfProperties getProperties(String className) throws Exception;

	/**
	 * Reads information of this property
	 * @param predicate
	 * @return
	 * @throws Exception
	 */
	public RdfProperty getProperty(Predicate predicate) throws Exception;

	/**
	 * Reads rdf:_li values of rdf:Alt
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	public List<RdfProperty> getAltValues(Qname qname) throws Exception;

	/**
	 * Return model by qname
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	public Model get(String qname) throws Exception;

	/**
	 * Return model by qname
	 * @param qname
	 * @return
	 * @throws Exception
	 */
	public Model get(Qname qname) throws Exception;

	/**
	 * Adds model to db or updates the db to match the given model. Those statements of the model that already exist in the db are left unchanged.
	 * @param model
	 * @throws Exception 
	 */
	public void store(Model model) throws Exception;

	/**
	 * Deletes all predicates of the given resource
	 * @param subject
	 * @throws Exception
	 */
	public void delete(Subject subject) throws Exception;

	/**
	 * Delete one statement by id - will not fail if statement does not exist
	 * @param id
	 * @throws Exception
	 */
	public void deleteStatement(int id) throws Exception;
	
	/**
	 * Deletes single predicates of the given resource from default context with no language
	 * @param subject
	 * @param predicate
	 * @throws SQLException
	 */
	public void delete(Subject subject, Predicate predicate) throws SQLException;

	/**
	 * Deletes single predicates of the given resource from the given context with no language
	 * @param subject
	 * @param predicate
	 * @param context null for default context
	 * @throws SQLException
	 */
	public void delete(Subject subject, Predicate predicate, Context context) throws SQLException;

	/**
	 * Removes used predicates from db and adds given statements.
	 * @param usedAndGivenStatements
	 * @throws SQLException 
	 */
	public void store(Subject subject, UsedAndGivenStatements usedAndGivenStatements) throws Exception;

	/**
	 * Adds or modifies a publiction. For new publication the ID must be already set.
	 * @param publication
	 * @return the same publication
	 * @throws Exception 
	 */
	public Publication storePublication(Publication publication) throws Exception;

	/**
	 * Gets taxon concept qname from sequence and adds the resource.
	 * @return
	 * @throws Exception
	 */
	public Qname addTaxonConcept() throws Exception;

	/**
	 * Clears for example Properties cache
	 */
	public void clearCaches();

	/**
	 * Updates occurrence information, making the neccessary deletions, updates and inserts.
	 * @param existingOccurrences
	 * @param newOccurrences
	 * @throws Exception
	 */
	public void store(Occurrences existingOccurrences, Occurrences newOccurrences) throws Exception;

	/**
	 * Insert or update occurrence. ID of occurrence may or may not be set for insert; if not given it will be generated and set to object.
	 * @param taxonQname
	 * @param occurrence
	 * @throws Exception
	 */
	public void store(Qname taxonQname, Occurrence occurrence) throws Exception;
	
	/**
	 * Get search dao
	 * @return
	 */
	public TriplestoreSearchDAO getSearchDAO();

	/**
	 * Get user resourceid
	 * @param userQname
	 * @return
	 * @throws Exception 
	 */
	public int getUserFK(String userQname) throws Exception;

	/**
	 * Opens connection that should be closed.
	 * @return
	 * @throws Exception
	 */
	public TransactionConnection openConnection() throws Exception;

	/**
	 * Adds a resource. Has no effect if resource already exists.
	 * @param qname
	 * @throws SQLException 
	 */
	public Qname addResource(Qname qname) throws Exception;

	public List<ResourceListing> getResourceStats() throws Exception;

	/**
	 * Checks if resource exists.
	 * @param qname
	 * @return
	 * @throws SQLException 
	 */
	public boolean resourceExists(String qname) throws Exception;

	/**
	 * Checks if resource exists.
	 * @param resourceQname
	 * @return
	 * @throws SQLException 
	 */
	public boolean resourceExists(Qname resourceQname) throws Exception;

	/**
	 * Store IUCN evaluation
	 * @param givenData
	 * @param existingEvaluation
	 * @throws Exception
	 */
	public void store(IUCNEvaluation givenData, IUCNEvaluation existingEvaluation) throws Exception;
	
}
