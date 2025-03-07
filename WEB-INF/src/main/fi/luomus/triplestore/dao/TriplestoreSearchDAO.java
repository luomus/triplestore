package fi.luomus.triplestore.dao;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

public interface TriplestoreSearchDAO {

	/**
	 * Search by combination of subjects, predicates, objects and rdf_type with limit and offset
	 * @param searchParams
	 * @return matching models
	 * @throws Exception
	 */
	public Collection<Model> search(SearchParams searchParams) throws Exception;

	/**
	 *  Search by predicate and objectresource using default limit and no offset 
	 * @param predicate
	 * @param objectresource
	 * @return
	 * @throws Exception
	 */
	public Collection<Model> search(String predicate, String objectresource) throws Exception;
	
	/**
	 * Return models by qnames using the defined ResultType
	 * @param qnames
	 * @param resultType
	 * @return
	 * @throws Exception unknown condition
	 * @throws TooManyResultsException if using result type on this resource provides too many results
	 */
	public Collection<Model> get(Set<Qname> qnames, ResultType resultType) throws TooManyResultsException, Exception;

	/**
	 * Get models by qnames
	 * @param subjects
	 * @return
	 * @throws Exception
	 */
	public Collection<Model> get(Set<Qname> subjects) throws Exception;
	
	/**
	 * Return count of models ignoring limit/offset
	 * @param searchParams
	 * @return
	 * @throws Exception 
	 */
	public int count(SearchParams searchParams) throws Exception;

	/**
	 * Search by combination of subjects, predicates, objects and rdf_type with limit and offset.
	 * @param searchParams
	 * @return matching subject qnames
	 * @throws Exception
	 */
	public Set<Qname> searchQnames(SearchParams searchParams) throws SQLException;

	
		
}
