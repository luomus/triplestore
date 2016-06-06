package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.taxonomy.models.IUCNContainer;

import java.util.List;
import java.util.Map;

public interface IucnDAO {

	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, List<Qname>> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public IUCNContainer getIUCNContainer();

}
