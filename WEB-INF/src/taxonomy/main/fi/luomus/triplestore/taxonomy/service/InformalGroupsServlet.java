package fi.luomus.triplestore.taxonomy.service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/taxonomy-editor/informalGroups/*", "/taxonomy-editor/informalGroups/add/*"})
public class InformalGroupsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -7740342063755041600L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		if (req.getRequestURI().endsWith("/informalGroups")) {
			Map<String, InformalTaxonGroup> informalGroups = getTaxonomyDAO().getInformalTaxonGroupsForceReload();
			Set<String> roots = getRoots(informalGroups);
			responseData.setData("informalGroups", informalGroups);
			responseData.setData("roots", roots);
			return responseData.setViewName("informalGroups");
		}
		if (addNew(req)) {
			return responseData.setViewName("informalGroups-edit").setData("action", "add").setData("group", new InformalTaxonGroup());
		}
		String qname = getQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroupsForceReload().get(qname);
		if (group == null) {
			return redirectTo404(res);
		}
		return responseData.setViewName("informalGroups-edit").setData("action", "modify").setData("group", group);
	}

	private Set<String> getRoots(Map<String, InformalTaxonGroup> informalGroups) {
		Set<String> allGroups = new LinkedHashSet<>(informalGroups.keySet());
		for (InformalTaxonGroup group : informalGroups.values()) {
			for (Qname subGroup : group.getSubGroups()) {
				allGroups.remove(subGroup.toString());
			}
		}
		return allGroups;
	}

	private boolean addNew(HttpServletRequest req) {
		return req.getRequestURI().endsWith("/add");
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		boolean addNew = addNew(req);
		TriplestoreDAO triplestoreDAO = getTriplestoreDAO(req);
		Qname qname = addNew ? triplestoreDAO.getSeqNextValAndAddResource("MVL") : new Qname(getQname(req));

		String nameEN = req.getParameter("name_en");
		String nameFI = req.getParameter("name_fi");
		String nameSV = req.getParameter("name_sv");

		if (!given(nameEN, nameFI, nameSV)) throw new IllegalStateException("Required parameters were not set");

		nameEN = Utils.upperCaseFirst(nameEN.toLowerCase());
		nameFI = Utils.upperCaseFirst(nameFI.toLowerCase());
		nameSV = Utils.upperCaseFirst(nameSV.toLowerCase());

		LocalizedText names = new LocalizedText();
		names.set("en", nameEN);
		names.set("fi", nameFI);
		names.set("sv", nameSV);

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
			return redirectTo(getConfig().baseURL()+"/informalGroups", res);
		} else {
			getSession(req).setFlashSuccess("Informal group modified");
			return redirectTo(getConfig().baseURL()+"/informalGroups/"+qname, res);
		}
	}

}
