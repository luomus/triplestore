package fi.luomus.triplestore.taxonomy.dao;

import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;

public interface IucnDAO {

	public static final Predicate SECONDARY_HABITAT_PREDICATE = new Predicate(IUCNEvaluation.SECONDARY_HABITAT);
	public static final Predicate PRIMARY_HABITAT_PREDICATE = new Predicate(IUCNEvaluation.PRIMARY_HABITAT);
	public static final Predicate HABITAT_PREDICATE = new Predicate(IUCNEvaluation.HABITAT);
	public static final Predicate HABITAT_SPESIFIC_TYPE_PREDICATE = new Predicate(IUCNEvaluation.HABITAT_SPECIFIC_TYPE);
	public static final Predicate HAS_OCCURRENCE_PREDICATE = new Predicate(IUCNEvaluation.HAS_OCCURRENCE);
	public static final Predicate HAS_ENDANGERMENT_REASON_PREDICATE = new Predicate(IUCNEvaluation.HAS_ENDANGERMENT_REASON);
	public static final Predicate HAS_THREATH_PREDICATE = new Predicate(IUCNEvaluation.HAS_THREAT);
	public static final Predicate PUBLICATION_PREDICATE = new Predicate(IUCNEvaluation.PUBLICATION);
	public static final Predicate EVALUATION_YEAR_PREDICATE = new Predicate(IUCNEvaluation.EVALUATION_YEAR);
	public static final Predicate EDIT_NOTES_PREDICATE = new Predicate(IUCNEvaluation.EDIT_NOTES);
	public static final Predicate EVALUATED_TAXON_PREDICATE = new Predicate(IUCNEvaluation.EVALUATED_TAXON);
	
	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, IUCNEditors> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public IUCNContainer getIUCNContainer();

	public List<String> getFinnishSpecies(String taxonQname) throws Exception;

	public Map<String, Area> getEvaluationAreas() throws Exception;

	public IUCNEvaluation createNewEvaluation() throws Exception;

	public IUCNEvaluation createEvaluation(String id) throws Exception;

	public Qname getSeqNextValAndAddResource() throws Exception;

	public EditHistory getEditHistory(IUCNEvaluation evaluation) throws Exception;

	public void completeLoading(IUCNEvaluation evaluation) throws Exception;

	public void moveEvaluation(String fromTaxonId, String toTaxonId, int year) throws Exception;
	
	public void deleteEvaluation(String fromTaxonId, int year) throws Exception;

}
