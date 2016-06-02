package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.services.ResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class IUCNFrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		
		List<Integer> evaluationYears = new ArrayList<>();
		for (Model m : getTriplestoreDAO().getSearchDAO().search("rdf:type", "MKV.iucnRedListEvaluationYear")) {
			int year = Integer.valueOf(m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent());
			evaluationYears.add(year);
		}
		Collections.sort(evaluationYears);
		int draftYear = Iterables.getLast(evaluationYears);
		
		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", draftYear)
				.setData("selectedYear", draftYear) // TODO
				.setData("checklist", getTaxonomyDAO().getChecklists().get("MR.1")); 
	}

}
