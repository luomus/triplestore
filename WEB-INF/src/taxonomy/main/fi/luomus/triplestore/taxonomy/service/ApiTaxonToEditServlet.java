package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/taxonToEdit/*"})
public class ApiTaxonToEditServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 3287946714651341429L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = getQname(req).replace("MX", "MX.");
		ResponseData responseData = initResponseData(req).setViewName("api-taxonedit");

		ExtendedTaxonomyDAO dao = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) dao.getTaxon(new Qname(taxonQname));

		try {
			checkPermissionsToAlterTaxon(taxon, req);
		} catch (IllegalAccessException noAcess) {
			responseData.setData("noPermissions", "true");
		}

		dao.addOccurrences(taxon);
		
		boolean isChecklistTaxon = isChecklistTaxon(taxon);
		boolean fullView = isChecklistTaxon || getUser(req).isAdmin();
		return responseData.setData("taxon", taxon).setData("fullView", fullView).setData("isChecklistTaxon", isChecklistTaxon);
	}

	private boolean isChecklistTaxon(EditableTaxon taxon) {
		return given(taxon.getChecklist());
	}

}
