package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.service.EditorBaseServlet;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.utils.NameCleaner;

public abstract class TaxonomyEditorBaseServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 6941260573571219110L;
	private static final int TAXON_DELETE_THRESHOLD_SECONDS = 60*60*5;
	private static final Object LOCK = new Object();
	private static ExtendedTaxonomyDAOImple taxonomyDAO;

	@Override
	protected String configFileName() {
		return "triplestore-v2-taxonomyeditor.properties";
	}

	@Override
	protected void applicationInit() {}

	@Override
	protected void applicationInitOnlyOnce() {
	}

	@Override
	protected void applicationDestroy() {
		if (taxonomyDAO != null) {
			taxonomyDAO.close();
		}
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return initResponseData(req);
	}

	private static final NameCleaner nameCleaner = new NameCleaner();

	@Override
	protected ResponseData initResponseData(HttpServletRequest req) throws Exception {
		ResponseData responseData = new ResponseData().setDefaultLocale("en");
		
		SessionHandler session = getSession(req);
		if (session.hasSession() && session.isAuthenticatedFor("triplestore")) {
			User user = getUser(session);
			responseData.setData("user", user);
			responseData.setData("flashMessage", session.getFlash());
			responseData.setData("successMessage", session.getFlashSuccess());
			responseData.setData("errorMessage", session.getFlashError());
			responseData.setData("restartMessage", getRestartMessage(user));
		}
		
		String synonymsMode = req.getParameter("synonymsMode"); 
		if (given(synonymsMode)) {
			responseData.setData("synonymsMode", synonymsMode);
		} else {
			responseData.setData("synonymsMode", "show");
		}
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		TriplestoreDAO dao = getTriplestoreDAO();

		responseData.setData("checklists", taxonomyDAO.getChecklists());
		responseData.setData("persons", taxonomyDAO.getPersons());
		responseData.setData("publications", taxonomyDAO.getPublications());
		responseData.setData("informalGroups", taxonomyDAO.getInformalTaxonGroups());
		responseData.setData("properties", dao.getProperties("MX.taxon"));
		responseData.setData("occurrenceProperties", dao.getProperties("MO.occurrence"));
		responseData.setData("biogeographicalProvinces", taxonomyDAO.getBiogeographicalProvinces());
		responseData.setData("lastAllowedTaxonDeleteTimestamp", getLastAllowedTaxonDeleteTimestamp());
		responseData.setData("nameCleaner", nameCleaner);
		responseData.setData("kotkaURL", getConfig().get("KotkaURL"));
		responseData.setData("evaluationYears", taxonomyDAO.getIucnDAO().getEvaluationYears());
		responseData.setData("redListStatusProperty", dao.getProperty(new Predicate(IUCNEvaluation.RED_LIST_STATUS)));
		return responseData;
	}

	public static long getLastAllowedTaxonDeleteTimestamp() {
		return DateUtils.getCurrentEpoch() - TAXON_DELETE_THRESHOLD_SECONDS;
	}

	protected ExtendedTaxonomyDAO getTaxonomyDAO() {
		if (taxonomyDAO == null) {
			synchronized (LOCK) {
				if (taxonomyDAO == null) {
					TriplestoreDAOConst.SCHEMA = getConfig().get("LuontoDbName");
					taxonomyDAO = new ExtendedTaxonomyDAOImple(this.getConfig(), getTriplestoreDAO(), getErrorReporter());
				}
			}
		}
		return taxonomyDAO;
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	protected EditableTaxon createTaxon(String scientificName, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		EditableTaxon taxon = taxonomyDAO.createTaxon();
		if (given(scientificName)) {
			taxon.setScientificName(scientificName);
		}
		return taxon;
	}

	protected void checkPermissionsToAlterTaxon(EditableTaxon taxon, HttpServletRequest req) throws Exception {
		User user = getUser(req);
		if (!taxon.allowsAlterationsBy(user)) {
			throw new IllegalAccessException("Person " + user.getFullname() + " (" + user.getQname() +") does not have permissions to alter taxon " + taxon.getScientificName() + " (" + taxon.getQname() + ")");
		}
	}

	protected void checkPermissionsToAlterTaxon(String taxonQname, HttpServletRequest req) throws Exception {
		checkPermissionsToAlterTaxon(new Qname(taxonQname), req);
	}

	protected void checkPermissionsToAlterTaxon(Qname taxonQname, HttpServletRequest req) throws Exception {
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(taxonQname);
		checkPermissionsToAlterTaxon(taxon, req);
	}

	protected boolean hasIucnPermissions(String groupQname, HttpServletRequest req) throws Exception {
		IUCNEditors editors = getTaxonomyDAO().getIucnDAO().getGroupEditors().get(groupQname);
		if (editors == null || editors.getEditors().isEmpty()) return false;
		User user = getUser(req);
		if (user == null || user.getQname() == null) return false;
		return editors.getEditors().contains(user.getQname());
	}

	protected void checkIucnPermissions(String groupQname, HttpServletRequest req) throws Exception {
		if (!hasIucnPermissions(groupQname, req)) {
			User user = getUser(req);
			throw new IllegalAccessException("Person " + user.getFullname() + " (" + user.getQname() +") does not have permissions to alter iucn group " + groupQname);
		}
	}

	protected Collection<EditableTaxon> parseAndCreateNewTaxons(HttpServletRequest req, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		List<EditableTaxon> taxons = new ArrayList<>();
		Map<Integer, Map<String, String>> newTaxonValues = parseNewTaxonValues(req);
		for (Map<String, String> taxonData : newTaxonValues.values()) {
			String sciName = taxonData.get("scientificName");
			if (!given(sciName)) continue;
			EditableTaxon taxon = createTaxon(sciName, taxonomyDAO);
			taxon.setScientificNameAuthorship(taxonData.get("authors"));
			if (taxonData.containsKey("rank")) {
				taxon.setTaxonRank(new Qname(taxonData.get("rank")));
			}
			taxon.setTaxonConceptQname(dao.addTaxonConcept());
			dao.addTaxon(taxon);
			taxons.add(taxon);
		}
		return taxons;
	}

	private Map<Integer, Map<String, String>> parseNewTaxonValues(HttpServletRequest req) {
		Map<Integer, Map<String, String>> map = new HashMap<>();
		Enumeration<String> e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String parameter = e.nextElement();
			if (!parameter.contains("___")) continue;
			String value = req.getParameter(parameter);
			if (!given(value)) continue;
			String[] parts = parameter.split(Pattern.quote("___"));
			int index = Integer.valueOf(parts[1]);
			String field = parts[0];
			if (!map.containsKey(index)) map.put(index, new HashMap<String, String>());
			map.get(index).put(field, value);
		}
		return map;
	}
	
}
