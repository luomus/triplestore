package fi.luomus.triplestore.taxonomy.dao;

import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.Editors;
import fi.luomus.commons.taxonomy.iucn.Evaluation;

public interface IucnDAO {

	public static final Predicate SECONDARY_HABITAT_PREDICATE = new Predicate(Evaluation.SECONDARY_HABITAT);
	public static final Predicate PRIMARY_HABITAT_PREDICATE = new Predicate(Evaluation.PRIMARY_HABITAT);
	public static final Predicate HABITAT_PREDICATE = new Predicate(Evaluation.HABITAT);
	public static final Predicate HABITAT_SPESIFIC_TYPE_PREDICATE = new Predicate(Evaluation.HABITAT_SPECIFIC_TYPE);
	public static final Predicate HAS_OCCURRENCE_PREDICATE = new Predicate(Evaluation.HAS_OCCURRENCE);
	public static final Predicate HAS_ENDANGERMENT_REASON_PREDICATE = new Predicate(Evaluation.HAS_ENDANGERMENT_REASON);
	public static final Predicate HAS_THREATH_PREDICATE = new Predicate(Evaluation.HAS_THREAT);
	public static final Predicate PUBLICATION_PREDICATE = new Predicate(Evaluation.PUBLICATION);
	public static final Predicate EVALUATION_YEAR_PREDICATE = new Predicate(Evaluation.EVALUATION_YEAR);
	public static final Predicate EDIT_NOTES_PREDICATE = new Predicate(Evaluation.EDIT_NOTES);
	public static final Predicate EVALUATED_TAXON_PREDICATE = new Predicate(Evaluation.EVALUATED_TAXON);
	
	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, Editors> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public Container getIUCNContainer();

	public List<String> getFinnishSpecies(String taxonQname) throws Exception;

	public Map<String, Area> getEvaluationAreas() throws Exception;

	public Evaluation createNewEvaluation() throws Exception;

	public Evaluation createEvaluation(String id) throws Exception;

	public Qname getSeqNextValAndAddResource() throws Exception;

	public EditHistory getEditHistory(Evaluation evaluation) throws Exception;

	public void completeLoading(Evaluation evaluation) throws Exception;

	public void moveEvaluation(String fromTaxonId, String toTaxonId, int year) throws Exception;
	
	public void deleteEvaluation(String fromTaxonId, int year) throws Exception;

}
