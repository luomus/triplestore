package fi.luomus.triplestore.taxonomy.service;

import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.service.EditorBaseServlet;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.Editors;
import fi.luomus.triplestore.taxonomy.iucn.model.Evaluation;
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

	private static final Set<User.Role> DEFAULT_ALLOWED = Collections.unmodifiableSet(Utils.set(User.Role.ADMIN, User.Role.NORMAL_USER));

	protected Set<User.Role> allowedRoles() {
		return DEFAULT_ALLOWED;
	}

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return allowedRoles().contains(getUser(req).getRole());
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

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		TriplestoreDAO dao = getTriplestoreDAO();

		responseData.setData("checklists", taxonomyDAO.getChecklists());
		responseData.setData("persons", taxonomyDAO.getPersons());
		responseData.setData("publications", taxonomyDAO.getPublications());
		responseData.setData("informalGroups", taxonomyDAO.getInformalTaxonGroups());
		responseData.setData("properties", dao.getProperties("MX.taxon"));
		responseData.setData("occurrenceProperties", dao.getProperties("MO.occurrence"));
		responseData.setData("biogeographicalProvinces", taxonomyDAO.getBiogeographicalProvinces());
		responseData.setData("nameCleaner", nameCleaner);
		responseData.setData("kotkaURL", getConfig().get("KotkaURL"));
		responseData.setData("evaluationYears", taxonomyDAO.getIucnDAO().getEvaluationYears());
		responseData.setData("redListStatusProperty", dao.getProperty(new Predicate(Evaluation.RED_LIST_STATUS)));
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
		return status404(res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
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
		Editors editors = getTaxonomyDAO().getIucnDAO().getGroupEditors().get(groupQname);
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

}
