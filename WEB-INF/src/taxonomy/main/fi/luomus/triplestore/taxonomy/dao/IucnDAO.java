package fi.luomus.triplestore.taxonomy.dao;

import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNRegionalStatus;

public interface IucnDAO {

	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, IUCNEditors> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public IUCNContainer getIUCNContainer();

	public List<String> getFinnishSpecies(String taxonQname) throws Exception;

	public Map<String, Area> getEvaluationAreas() throws Exception;

	public IUCNEvaluation createNewEvaluation() throws Exception;

	public IUCNEvaluation createEvaluation(String id) throws Exception;

	public Qname getSeqNextValAndAddResource() throws Exception;

	public void store(IUCNHabitatObject primaryHabitat) throws Exception;
	
	public void store(IUCNRegionalStatus regionalStatus) throws Exception;

	public void store(IUCNEndangermentObject endangermentObject) throws Exception;
	
	public EditHistory getEditHistory(IUCNEvaluation evaluation) throws Exception;

}
