package fi.luomus.triplestore.dao;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

import java.util.Collection;
import java.util.List;

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
	public Collection<Model> get(List<Qname> qnames, ResultType resultType) throws TooManyResultsException, Exception;
		
}
