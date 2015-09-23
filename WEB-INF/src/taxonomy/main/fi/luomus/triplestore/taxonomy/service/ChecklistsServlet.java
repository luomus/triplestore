package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/checklists/*", "/taxonomy-editor/checklists/add/*"})
public class ChecklistsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -8012379195657744561L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		if (req.getRequestURI().endsWith("/checklists")) {
			responseData.setData("checklists", getTaxonomyDAO().getChecklistsForceReload());
			return responseData.setViewName("checklists");	
		}
		if (addNew(req)) {
			return responseData.setViewName("checklists-edit").setData("action", "add").setData("checklist", new Checklist());
		}
		String qname = getQname(req);
		Checklist checklist = getTaxonomyDAO().getChecklistsForceReload().get(qname);
		if (checklist == null) {
			return redirectTo404(res);
		}
		return responseData.setViewName("checklists-edit").setData("action", "modify").setData("checklist", checklist);
	}

	private boolean addNew(HttpServletRequest req) {
		return req.getRequestURI().endsWith("/add");
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		boolean addNew = addNew(req);
		TriplestoreDAO triplestoreDAO = getTriplestoreDAO(req);
		Qname qname = addNew ? triplestoreDAO.getSeqNextValAndAddResource("MR") : new Qname(getQname(req));
		String nameEN = req.getParameter("name_en");
		String nameFI = req.getParameter("name_fi");
		String notesEN = req.getParameter("notes_en");
		String notesFI = req.getParameter("notes_fi");
		String owner = req.getParameter("owner");
		String isPublic = req.getParameter("isPublic");
		String createRoot = req.getParameter("createRoot");
		String rootTaxonQname = req.getParameter("rootTaxonQname");

		LocalizedText names = new LocalizedText();
		names.set("en", nameEN);
		names.set("fi", nameFI);

		if (createRootTaxon(createRoot)) {
			Taxon rootTaxon = createTaxon(qname, triplestoreDAO);
			rootTaxonQname = rootTaxon.getQname().toString();
		}

		Checklist checklist = new Checklist(qname, names, rootTaxonQname == null ? null : new Qname(rootTaxonQname));

		LocalizedText notes = new LocalizedText();
		notes.set("en", notesEN);
		notes.set("fi", notesFI);
		checklist.setNotes(notes);

		if ("true".equals(isPublic)) {
			checklist.setPublic(true);
		} else if ("false".equals(isPublic)) {
			checklist.setPublic(false);
		}
		if (given(owner)) {
			checklist.setOwner(new Qname(owner));
		}

		triplestoreDAO.storeChecklist(checklist);
		getTaxonomyDAO().getChecklistsForceReload();

		if (addNew) {
			getSession(req).setFlashSuccess("New checklist added");
			return redirectTo(getConfig().baseURL()+"/checklists", res);
		} else {
			getSession(req).setFlashSuccess("Checklist modified");
			return redirectTo(getConfig().baseURL()+"/checklists/"+qname, res);
		}
	}

	private boolean createRootTaxon(String createRoot) {
		return "yes".equals(createRoot);
	}

	private Taxon createTaxon(Qname checklistQname, TriplestoreDAO dao) throws Exception {
		Taxon taxon = createTaxonAndFetchNextId("Rename me!", dao);
		taxon.setChecklist(checklistQname);
		dao.addTaxon(taxon);
		return taxon;
	}

}
