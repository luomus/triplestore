package fi.luomus.triplestore.taxonomy.iucn.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.HabitatLabelIndendator;
import fi.luomus.triplestore.taxonomy.iucn.model.Editors;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class FrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		int selectedYear = selectedYear(req);
		List<Integer> evaluationYears = getTaxonomyDAO().getIucnDAO().getEvaluationYears();
		Map<String, Editors> groupEditors = getTaxonomyDAO().getIucnDAO().getGroupEditors();
		TriplestoreDAO dao = getTriplestoreDAO();
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", getDraftYear(evaluationYears))
				.setData("selectedYear", selectedYear)
				.setData("taxonGroups", taxonomyDAO.getInformalTaxonGroups())
				.setData("taxonGroupRoots", taxonomyDAO.getInformalTaxonGroupRoots())
				.setData("taxonGroupEditors", groupEditors)
				.setData("evaluationProperties", dao.getProperties(Evaluation.EVALUATION_CLASS))
				.setData("habitatObjectProperties", dao.getProperties(Evaluation.HABITAT_OBJECT_CLASS))
				.setData("endangermentObjectProperties", dao.getProperties(Evaluation.ENDANGERMENT_OBJECT_CLASS))
				.setData("areas", iucnDAO.getEvaluationAreas())
				.setData("regionalOccurrenceStatuses", getRegionalOccurrenceStatuses())
				.setData("occurrenceStatuses", getOccurrenceStatuses())
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate(Evaluation.RED_LIST_STATUS)))
				.setData("downloads", getCompletedDownloads());
	}

	private List<String> getCompletedDownloads() {
		List<String> files = new ArrayList<>();
		try {
			File folder = new File(getConfig().reportFolder());
			folder.mkdirs();
			for (File f : folder.listFiles()) {
				if (f.getName().endsWith(".zip")) {
					files.add(f.getName());
				}
			}
			Collections.reverse(files);
		} catch (Exception e) {
			getErrorReporter().report(e);
		}
		return files;
	}

	protected int selectedYear(HttpServletRequest req) throws Exception {
		List<Integer> evaluationYears = getTaxonomyDAO().getIucnDAO().getEvaluationYears(); 
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			return getDraftYear(evaluationYears);
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			if (!evaluationYears.contains(selectedYear)) throw new IllegalArgumentException();
			return selectedYear;
		} catch (Exception e) {
			return getDraftYear(getTaxonomyDAO().getIucnDAO().getEvaluationYears());
		}
	}

	protected int selectedYearFailForNoneGiven(HttpServletRequest req) throws Exception {
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			throw new IllegalArgumentException("Must give evaluation year.");
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			List<Integer> evaluationYears = getTaxonomyDAO().getIucnDAO().getEvaluationYears();
			if (!evaluationYears.contains(selectedYear)) throw new IllegalArgumentException();
			return selectedYear;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid evaluation year: " + selectedYearParam);
		}
	}

	private int getDraftYear(List<Integer> allYears) throws Exception {
		return Iterables.getLast(allYears);
	}

	private static Collection<RdfProperty> occurrenceStatuses;
	private static Collection<RdfProperty> regionalOccurrenceStatuses;

	protected Collection<RdfProperty> getRegionalOccurrenceStatuses() throws Exception {
		if (regionalOccurrenceStatuses == null) {
			regionalOccurrenceStatuses = initRegionalOccurrenceStatuses();
		}
		return regionalOccurrenceStatuses;
	}

	private List<RdfProperty> initRegionalOccurrenceStatuses() throws Exception {
		List<RdfProperty> statuses = new ArrayList<>();
		Collection<RdfProperty> referenceStatuses = getTriplestoreDAO().getProperty(new Predicate("MO.status")).getRange().getValues();
		statuses.add(buildOccurrenceStatus("MX.typeOfOccurrenceOccurs", "Esiintyy vyöhykkeellä", referenceStatuses));
		statuses.add(buildOccurrenceStatus("MX.typeOfOccurrenceExtirpated", "Hävinnyt vyöhykkeeltä (RE)", referenceStatuses));
		statuses.add(buildOccurrenceStatus("MX.typeOfOccurrenceAnthropogenic", "Satunnainen tai ihmisen avustamana vyöhykkeelle siirtynyt (NA)", referenceStatuses));
		statuses.add(buildOccurrenceStatus("MX.typeOfOccurrenceUncertain", "Esiintyy mahdollisesti vyöhykkeellä (epävarma)", referenceStatuses));
		statuses.add(buildOccurrenceStatus("MX.doesNotOccur", "Ei havaintoja vyöhykkeeltä", referenceStatuses));
		return statuses;
	} 

	protected Collection<RdfProperty> getOccurrenceStatuses() throws Exception {
		if (occurrenceStatuses == null) {
			occurrenceStatuses = initOccurrenceStatuses();
		}
		return occurrenceStatuses;
	}

	private Collection<RdfProperty> initOccurrenceStatuses() throws Exception {
		List<RdfProperty> occurrences = new ArrayList<>();
		Collection<RdfProperty> referenceStatuses = getTriplestoreDAO().getProperty(new Predicate("MO.status")).getRange().getValues();
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceStablePopulation", "Vakiintunut", referenceStatuses));
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceNotEstablished", "Uusi laji", referenceStatuses));
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceExtirpated", "Hävinnyt", referenceStatuses));
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceVagrant", "Säännöllinen vierailija", referenceStatuses));
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceRareVagrant", "Satunnainen vierailija", referenceStatuses));
		occurrences.add(buildOccurrenceStatus("MX.typeOfOccurrenceAnthropogenic", "Vieraslaji", referenceStatuses));
		return occurrences;
	}

	private RdfProperty buildOccurrenceStatus(String id, String label, Collection<RdfProperty> referenceStatuses) {
		Qname qname = new Qname(id);
		if (!contains(referenceStatuses, qname)) throw new IllegalStateException("Unknown reference status: " + id);
		RdfProperty p = new RdfProperty(qname);
		p.setLabels(new LocalizedText().set("fi", label));
		return p;
	}

	private boolean contains(Collection<RdfProperty> referenceStatuses, Qname qname) {
		for (RdfProperty p : referenceStatuses) {
			if (p.getQname().equals(qname)) return true;
		}
		return false;
	}

	private static HabitatLabelIndendator habitatLabelIndendator = null;

	protected HabitatLabelIndendator getHabitatLabelIndentaror() throws Exception {
		if (habitatLabelIndendator == null) {
			habitatLabelIndendator = new HabitatLabelIndendator(getTriplestoreDAO().getProperty(IucnDAO.HABITAT_PREDICATE).getRange().getValues());
		}
		return habitatLabelIndendator;
	}

}
