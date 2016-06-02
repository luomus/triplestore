package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected.CacheLoader;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class IUCNFrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	protected static final String IUCN_NAMESPACE = "MKV";

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);

		List<Integer> evaluationYears = getEvaluationYears();
		int draftYear = Iterables.getLast(evaluationYears);
		int selectedYear = selectedYear(req, evaluationYears, draftYear);

		Collection<InformalTaxonGroup> groups = getTaxonomyDAO().getInformalTaxonGroups().values();
		Map<String, TaxonGroupIucnEditors> groupEditors = getGroupEditors();

		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", draftYear)
				.setData("selectedYear", selectedYear)
				.setData("checklist", getTaxonomyDAO().getChecklists().get("MR.1"))
				.setData("taxonGroups", groups)
				.setData("taxonGroupEditors", groupEditors); 
	}

	private static final SingleObjectCacheResourceInjected<Map<String, TaxonGroupIucnEditors>, TriplestoreDAO> 
	cachedGroupEditors = 
	new SingleObjectCacheResourceInjected<>(
			new CacheLoader<Map<String, TaxonGroupIucnEditors>, TriplestoreDAO>() {
				@Override
				public Map<String, TaxonGroupIucnEditors> load(TriplestoreDAO dao) {
					try {
						Map<String,TaxonGroupIucnEditors> map = new HashMap<>();
						for (Model m : dao.getSearchDAO().search("rdf:type", "MKV.taxonGroupIucnEditors")) {
							String groupQname = m.getStatements("MKV.taxonGroup").get(0).getObjectResource().getQname();
							TaxonGroupIucnEditors groupEditors = new TaxonGroupIucnEditors(new Qname(m.getSubject().getQname()), new Qname(groupQname));
							for (Statement editor : m.getStatements("MKV.iucnEditor")) {
								groupEditors.addEditor(new Qname(editor.getObjectResource().getQname()));
							}
							map.put(groupQname, groupEditors);
						}
						return map;
					} catch (Exception e) {
						throw new RuntimeException("Cached group editors", e);
					}
				}
			}, 2*60);

	protected Map<String, TaxonGroupIucnEditors> getGroupEditors() throws Exception {
		return cachedGroupEditors.get(getTriplestoreDAO());
	}

	protected void clearCaches() {
		cachedGroupEditors.invalidate();
		evaluationYearsCache.invalidate();
	}

	private int selectedYear(HttpServletRequest req, List<Integer> evaluationYears, int draftYear) {
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			return draftYear;
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			if (!evaluationYears.contains(selectedYear)) {
				return draftYear; 
			}
			return selectedYear;
		} catch (Exception e) {
			return draftYear;
		}
	}

	private static final SingleObjectCacheResourceInjected<List<Integer>, TriplestoreDAO> 
	evaluationYearsCache = 
	new SingleObjectCacheResourceInjected<>(
			new CacheLoader<List<Integer>, TriplestoreDAO>() {
				@Override
				public List<Integer> load(TriplestoreDAO dao) {
					List<Integer> evaluationYears = new ArrayList<>();
					try {
						for (Model m : dao.getSearchDAO().search("rdf:type", "MKV.iucnRedListEvaluationYear")) {
							int year = Integer.valueOf(m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent());
							evaluationYears.add(year);
						}
					} catch (Exception e) {
						throw new RuntimeException("Evaluation years cache", e);
					}
					Collections.sort(evaluationYears);
					return evaluationYears;
				}
			}, 60*2);

	private List<Integer> getEvaluationYears() throws Exception {
		return evaluationYearsCache.get(getTriplestoreDAO());
	}

}
