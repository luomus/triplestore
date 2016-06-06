package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData;

import java.util.List;
import java.util.Map;

public interface IucnDAO {

	public List<Integer> getEvaluationYears() throws Exception;

	public Map<String, TaxonGroupIucnEditors> getGroupEditors() throws Exception;

	public void clearCaches();

	public TaxonGroupIucnEvaluationData getTaxonGroupData(String groupQname) throws Exception;

}
