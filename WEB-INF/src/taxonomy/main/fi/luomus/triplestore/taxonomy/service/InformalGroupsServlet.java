package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/informalGroups/*", "/taxonomy-editor/informalGroups/add/*"})
public class InformalGroupsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -7740342063755041600L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		if (req.getRequestURI().endsWith("/informalGroups")) {
			responseData.setData("informalGroups", getTaxonomyDAO().getInformalGroupsForceReload());
			return responseData.setViewName("informalGroups");	
		}
		if (addNew(req)) {
			return responseData.setViewName("informalGroups-edit").setData("action", "add").setData("group", new InformalGroup());
		}
		String qname = getQname(req);
		InformalGroup group = getTaxonomyDAO().getInformalGroupsForceReload().get(qname);
		if (group == null) {
			return redirectTo404(res);
		}
		return responseData.setViewName("informalGroups-edit").setData("action", "modify").setData("group", group);
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
		String nameLA = req.getParameter("name_la");

		if (!given(nameEN, nameFI, nameSV, nameLA)) throw new IllegalStateException("Required parameters were not set");

		nameEN = Utils.upperCaseFirst(nameEN.toLowerCase());
		nameFI = Utils.upperCaseFirst(nameFI.toLowerCase());
		nameSV = Utils.upperCaseFirst(nameSV.toLowerCase());
		nameLA = Utils.upperCaseFirst(nameLA.toLowerCase());

		LocalizedText names = new LocalizedText();
		names.set("en", nameEN);
		names.set("fi", nameFI);
		names.set("sv", nameSV);
		names.set("la", nameLA);

		InformalGroup group = new InformalGroup(qname, names);

		triplestoreDAO.storeInformalGroup(group);
		getTaxonomyDAO().getInformalGroupsForceReload();

		if (addNew) {
			getSession(req).setFlashSuccess("New informal group added");
			return redirectTo(getConfig().baseURL()+"/informalGroups", res);
		} else {
			getSession(req).setFlashSuccess("Informal group modified");
			return redirectTo(getConfig().baseURL()+"/informalGroups/"+qname, res);
		}
	}

}
