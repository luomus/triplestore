package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

@WebServlet(urlPatterns = {"/taxonomy-editor/informalGroups/*", "/taxonomy-editor/informalGroups/add/*", "/taxonomy-editor/informalGroups/delete/*"})
public class InformalGroupsServlet extends TaxonomyEditorBaseServlet {

	private static final Qname FINBIF_MASTER_CHECKLIST = new Qname("MR.1");
	private static final long serialVersionUID = -7740342063755041600L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req);
		ExtendedTaxonomyDAO dao = getTaxonomyDAO();

		if (getUser(req).isAdmin()) {
			responseData.setData("informalGroups", dao.getInformalTaxonGroupsForceReload()); // for admin always reload; for others this is in initResponseData
		}

		if (req.getRequestURI().endsWith("/informalGroups")) {
			responseData.setData("roots", dao.getInformalTaxonGroupRoots());
			return responseData.setViewName("informalGroups");
		}

		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins");

		if (addNew(req)) {
			return responseData.setViewName("informalGroups-edit").setData("action", "add").setData("group", new InformalTaxonGroup());
		}

		String groupId = getQname(req);
		InformalTaxonGroup group = dao.getInformalTaxonGroupsForceReload().get(groupId);
		if (group == null) {
			return status404(res);
		}

		Set<Qname> definingTaxaIds = dao.getTaxonContainer().getInformalGroupFilter().getFilteredTaxons(new Qname(groupId));
		List<Taxon> definingTaxa = new ArrayList<>();
		int i = 0;
		for (Qname id : definingTaxaIds) {
			Taxon t = dao.getTaxon(id);
			if (t != null) {
				if (FINBIF_MASTER_CHECKLIST.equals(t.getChecklist())) {
					definingTaxa.add(t);
				}
			}
			if (i++ > 30) break; 
		}
		Collections.sort(definingTaxa);

		return responseData
				.setViewName("informalGroups-edit")
				.setData("action", "modify")
				.setData("group", group)
				.setData("definingTaxa", definingTaxa)
				.setData("hasMore", definingTaxa.size() < definingTaxaIds.size());
	}

	private boolean addNew(HttpServletRequest req) {
		return req.getRequestURI().endsWith("/add");
	}

	private boolean delete(HttpServletRequest req) {
		return req.getRequestURI().contains("/delete/");
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		boolean addNew = addNew(req);
		boolean delete = delete(req);

		TriplestoreDAO triplestoreDAO = getTriplestoreDAO(req);
		Qname qname = addNew ? triplestoreDAO.getSeqNextValAndAddResource("MVL") : new Qname(getQname(req));

		if (delete) {
			triplestoreDAO.delete(new Subject(qname));
			getTaxonomyDAO().getInformalTaxonGroupsForceReload();
			getSession(req).setFlashSuccess("Informal group deleted");
			return redirectTo(getConfig().baseURL()+"/informalGroups");
		}

		String nameEN = req.getParameter("name_en");
		String nameFI = req.getParameter("name_fi");
		String nameSV = req.getParameter("name_sv");

		if (!given(nameEN, nameFI, nameSV)) throw new IllegalStateException("Required parameters were not set");

		LocalizedText names = new LocalizedText();
		names.set("en", Utils.upperCaseFirst(nameEN));
		names.set("fi", Utils.upperCaseFirst(nameFI));
		names.set("sv", Utils.upperCaseFirst(nameSV));

		InformalTaxonGroup group = new InformalTaxonGroup(qname, names);

		if (req.getParameter("MVL.hasSubGroup") != null) {
			for (String subGroupQname : req.getParameterValues("MVL.hasSubGroup")) {
				group.addSubGroup(new Qname(subGroupQname));
			}
		}

		triplestoreDAO.storeInformalTaxonGroup(group);
		getTaxonomyDAO().getInformalTaxonGroupsForceReload();

		if (addNew) {
			getSession(req).setFlashSuccess("New informal group added");
			return redirectTo(getConfig().baseURL()+"/informalGroups");
		} else {
			getSession(req).setFlashSuccess("Informal group modified");
			return redirectTo(getConfig().baseURL()+"/informalGroups/"+qname);
		}
	}

}
