package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/taxonomy-editor/informalGroups/*", "/taxonomy-editor/informalGroups/add/*", "/taxonomy-editor/informalGroups/delete/*"})
public class InformalGroupsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -7740342063755041600L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);

		if (getUser(req).isAdmin()) {
			responseData.setData("informalGroups", getTaxonomyDAO().getInformalTaxonGroupsForceReload()); // for admin always reload; for others this is et in initResponseData
		}

		if (req.getRequestURI().endsWith("/informalGroups")) {
			responseData.setData("roots", getTaxonomyDAO().getInformalTaxonGroupRoots());
			return responseData.setViewName("informalGroups");
		}

		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins");

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

	private boolean addNew(HttpServletRequest req) {
		return req.getRequestURI().endsWith("/add");
	}

	private boolean delete(HttpServletRequest req) {
		return req.getRequestURI().contains("/delete/");
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		boolean addNew = addNew(req);
		boolean delete = delete(req);

		TriplestoreDAO triplestoreDAO = getTriplestoreDAO(req);
		Qname qname = addNew ? triplestoreDAO.getSeqNextValAndAddResource("MVL") : new Qname(getQname(req));

		if (delete) {
			triplestoreDAO.delete(new Subject(qname));
			getTaxonomyDAO().getInformalTaxonGroupsForceReload();
			getSession(req).setFlashSuccess("Informal group deleted");
			return redirectTo(getConfig().baseURL()+"/informalGroups", res);
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
			return redirectTo(getConfig().baseURL()+"/informalGroups", res);
		} else {
			getSession(req).setFlashSuccess("Informal group modified");
			return redirectTo(getConfig().baseURL()+"/informalGroups/"+qname, res);
		}
	}

}
