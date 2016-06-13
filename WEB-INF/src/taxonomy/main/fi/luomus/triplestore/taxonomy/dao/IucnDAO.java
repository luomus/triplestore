package fi.luomus.triplestore.taxonomy.dao;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;

import java.util.List;
import java.util.Map;

public interface IucnDAO {

	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, IUCNEditors> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public IUCNContainer getIUCNContainer();

	public List<String> getFinnishSpecies(String taxonQname) throws Exception;

	public Map<String, Area> getEvaluationAreas() throws Exception;

	public IUCNEvaluation createEvaluation() throws Exception;

	public Qname getSeqNextValAndAddResource() throws Exception;
}
