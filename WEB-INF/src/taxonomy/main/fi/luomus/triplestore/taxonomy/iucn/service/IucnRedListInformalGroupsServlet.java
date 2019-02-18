package fi.luomus.triplestore.taxonomy.iucn.service;

import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn-groups/*", "/taxonomy-editor/iucn-groups/add/*", "/taxonomy-editor/iucn-groups/delete/*"})
public class IucnRedListInformalGroupsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 8786096622779728257L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req);

		if (getUser(req).isAdmin()) {
			responseData.setData("groups", getTaxonomyDAO().getRedListEvaluationGroupsForceReload());
		} else {
			responseData.setData("groups", getTaxonomyDAO().getRedListEvaluationGroups());
		}

		if (req.getRequestURI().endsWith("/iucn-groups")) {
			responseData.setData("roots", getTaxonomyDAO().getIucnRedListInformalGroupRoots());
			return responseData.setViewName("iucnGroups");
		}

		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins");

		if (addNew(req)) {
			return responseData.setViewName("iucnGroups-edit").setData("action", "add").setData("group", new RedListEvaluationGroup());
		}

		String qname = getQname(req);
		RedListEvaluationGroup group = getTaxonomyDAO().getRedListEvaluationGroupsForceReload().get(qname);
		if (group == null) {
			return status404(res);
		}
		return responseData.setViewName("iucnGroups-edit").setData("action", "modify").setData("group", group);
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
			getTaxonomyDAO().getRedListEvaluationGroupsForceReload();
			getSession(req).setFlashSuccess("IUCN group deleted");
			return redirectTo(getConfig().baseURL()+"/iucn-groups");
		}

		String nameEN = req.getParameter("name_en");
		String nameFI = req.getParameter("name_fi");
		String nameSV = req.getParameter("name_sv");

		LocalizedText names = new LocalizedText();
		if (nameEN != null) {
			names.set("en", Utils.upperCaseFirst(nameEN));
		}
		if (nameFI != null) {
			names.set("fi", Utils.upperCaseFirst(nameFI));
		}
		if (nameSV != null) {
			names.set("sv", Utils.upperCaseFirst(nameSV));
		}

		int order = getSortOrder(req);

		RedListEvaluationGroup group = new RedListEvaluationGroup(qname, names, order);

		if (req.getParameter("MVL.hasIucnSubGroup") != null) {
			for (String subGroupQname : req.getParameterValues("MVL.hasIucnSubGroup")) {
				group.addSubGroup(new Qname(subGroupQname));
			}
		}

		if (req.getParameter("MVL.includesInformalTaxonGroup") != null) {
			for (String groupId : req.getParameterValues("MVL.includesInformalTaxonGroup")) {
				group.addInformalGroup(new Qname(groupId));
			}
		}

		if (req.getParameter("MVL.includesTaxon") != null) {
			for (String taxonIds : req.getParameterValues("MVL.includesTaxon")) {
				for (String taxonId : taxonIds.split(Pattern.quote(","))) {
					Qname id = new Qname(taxonId.trim());
					if (id.isSet()) {
						group.addTaxon(id);
					}
				}
			}
		}

		triplestoreDAO.storeIucnRedListTaxonGroup(group);
		getTaxonomyDAO().getRedListEvaluationGroupsForceReload();

		if (addNew) {
			getSession(req).setFlashSuccess("New IUCN group added");
			return redirectTo(getConfig().baseURL()+"/iucn-groups");
		} else {
			getSession(req).setFlashSuccess("IUCN group modified");
			return redirectTo(getConfig().baseURL()+"/iucn-groups");
		}
	}

	private int getSortOrder(HttpServletRequest req) {
		try {
			return Integer.valueOf(req.getParameter("sortOrder"));
		} catch (Exception e) {}
		return Integer.MAX_VALUE;
	}

}
