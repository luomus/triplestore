package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/*"})
public class TaxonomyTreesEditorServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 2891309144359367681L;
	public static final Qname DEFAULT_ROOT_QNAME = new Qname("MX.53761");

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("taxonEditMain");

		Qname root = getRootOrDefaultRootOrIfNonExistingQnameGivenReturnNull(req); 
		if (root == null) {
			return redirectTo404(res);
		}

		ExtendedTaxonomyDAO dao = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) dao.getTaxon(root);
		responseData.setData("taxon", taxon);
		responseData.setData("root", taxon);

		try {
			checkPermissionsToAlterTaxon(taxon, req);
		} catch (IllegalAccessException noAcess) {
			responseData.setData("noPermissions", "true");
		}

		if (taxon.getChecklist() != null) {
			responseData.setData("checklist", getTaxonomyDAO().getChecklists().get(taxon.getChecklist().toString()));
		}
		return responseData;
	}

	private Qname getRootOrDefaultRootOrIfNonExistingQnameGivenReturnNull(HttpServletRequest req) throws Exception {
		String qname = getQname(req);
		if (!given(qname)) {
			return DEFAULT_ROOT_QNAME;
		}
		if (checklistQname(qname)) {
			Checklist checklist = getTaxonomyDAO().getChecklists().get(qname);
			if (checklist == null || checklist.getRootTaxon() == null) return null;
			return checklist.getRootTaxon();
		}
		return new Qname(qname);
	}

	private boolean checklistQname(String qname) {
		return qname.startsWith("MR.");
	}

}
