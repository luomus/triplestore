package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.lajitietokeskus.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.lajitietokeskus.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.service.EditorBaseServlet;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.utils.NameCleaner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class TaxonomyEditorBaseServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 6941260573571219110L;

	private static final int TAXON_DELETE_THRESHOLD_SECONDS = 60*60*5;
	private static final Qname SANDBOX_CHECKLIST = new Qname("MR.176");

	private static ExtendedTaxonomyDAO taxonomyDAO;

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
	protected void applicationDestroy() {}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return initResponseData(req);
	}

	private static final NameCleaner nameCleaner = new NameCleaner();

	@Override
	protected ResponseData initResponseData(HttpServletRequest req) throws Exception {
		ResponseData responseData = super.initResponseData(req).setViewName("help");
		String synonymsMode = req.getParameter("synonymsMode"); 
		if (given(synonymsMode)) {
			responseData.setData("synonymsMode", synonymsMode);
		} else {
			responseData.setData("synonymsMode", "show");
		}
		responseData.setData("checklists", getTaxonomyDAO().getChecklists());
		responseData.setData("persons", getTaxonomyDAO().getPersons());
		responseData.setData("publications", getTaxonomyDAO().getPublications());
		responseData.setData("areas", getTaxonomyDAO().getAreas());
		responseData.setData("properties", getTriplestoreDAO().getProperties("MX.taxon"));
		responseData.setData("occurrenceProperties", getTriplestoreDAO().getProperties("MO.occurrence"));
		responseData.setData("lastAllowedTaxonDeleteTimestamp", getLastAllowedTaxonDeleteTimestamp());
		responseData.setData("nameCleaner", nameCleaner);
		responseData.setData("kotkaURL", getConfig().get("KotkaURL"));
		return responseData;
	}

	public static long getLastAllowedTaxonDeleteTimestamp() {
		return DateUtils.getCurrentEpoch() - TAXON_DELETE_THRESHOLD_SECONDS;
	}

	protected ExtendedTaxonomyDAO getTaxonomyDAO() {
		if (taxonomyDAO == null) {
			TriplestoreDAOConst.SCHEMA = getConfig().get("LuontoDbName");
			taxonomyDAO = new ExtendedTaxonomyDAOImple(this.getConfig(), getTriplestoreDAO());
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

	protected EditableTaxon createTaxonAndFetchNextId(String scientificName, TriplestoreDAO dao) throws Exception {
		Qname qname = dao.getSeqNextValAndAddResource("MX");
		EditableTaxon taxon = new EditableTaxon(qname, getTaxonomyDAO());
		if (given(scientificName)) {
			taxon.setScientificName(scientificName);
		}
		return taxon;
	}

	protected void checkPermissionsToAlterTaxon(EditableTaxon taxon, HttpServletRequest req) throws Exception {
		if (SANDBOX_CHECKLIST.equals(taxon.getChecklist())) return;
		User user = getUser(req);
		if (!taxon.allowsAlterationsBy(user)) {
			throw new IllegalAccessException("Person " + user.getFullname() + " (" + user.getAdUserID() +") does not have permissions to alter taxon " + taxon.getScientificName() + " (" + taxon.getQname() + ")");
		}
	}

	protected void checkPermissionsToAlterTaxon(String taxonQname, HttpServletRequest req) throws Exception {
		checkPermissionsToAlterTaxon(new Qname(taxonQname), req);
	}

	protected void checkPermissionsToAlterTaxon(Qname taxonQname, HttpServletRequest req) throws Exception {
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(taxonQname);
		checkPermissionsToAlterTaxon(taxon, req);
	}

}
