package fi.luomus.triplestore.dao;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

import java.util.Collection;
import java.util.List;

public interface TriplestoreSearchDAO {

	/**
	 * Search by combination of subjects, predicates, objects with limit and offset
	 * @param subjects
	 * @param predicates
	 * @param objects
	 * @param objectresources
	 * @param objectliterals
	 * @param type 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws Exception 
	 */
	public Collection<Model> search(String[] subjects, String[] predicates, String[] objects, String[] objectresources, String[] objectliterals, String type, int limit, int offset) throws Exception;

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
	public Collection<Model> get(List<Qname> qnames, ResultType resultType) throws TooManyResultsException, Exception;
		
}
