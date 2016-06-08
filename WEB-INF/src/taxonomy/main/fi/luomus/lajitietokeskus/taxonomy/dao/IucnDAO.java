package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.triplestore.taxonomy.models.IUCNContainer;
import fi.luomus.triplestore.taxonomy.models.IUCNEditors;

import java.util.List;
import java.util.Map;

public interface IucnDAO {

	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, IUCNEditors> getGroupEditors() throws Exception;

	public void clearEditorCache();

	public IUCNContainer getIUCNContainer();

}
