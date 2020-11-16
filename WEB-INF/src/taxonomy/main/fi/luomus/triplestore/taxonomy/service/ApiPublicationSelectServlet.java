package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/publicationSelect/*"})
public class ApiPublicationSelectServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -8163846652948798774L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req).setViewName("api-publicationSelect");

		String fieldName = req.getParameter("fieldName");
		if (!given(fieldName)) throw new IllegalArgumentException("Must give fieldName");

		String taxonQname = getId(req);
		if (!taxonQname.contains(".")) taxonQname = taxonQname.replace("MX", "MX.");
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));

		checkPermissionsToAlterTaxon(taxon, req);

		return responseData.setData("taxon", taxon).setData("fieldName", fieldName);
	}

}
