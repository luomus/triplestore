package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/hideTaxon/*", "/taxonomy-editor/api/unhideTaxon/*"})
public class ApiHideUnhideTaxon extends ApiBaseServlet {

	private static final long serialVersionUID = 8057985427638993882L;
	private static final Predicate HIDDEN_TAXON = new Predicate("MX.hiddenTaxon");

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			return status500(res);
		} 
		if (!taxonQname.contains(".")) taxonQname = taxonQname.replace("MX", "MX.");

		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(new Qname(taxonQname));
		checkPermissionsToAlterTaxon(taxon, req);

		TriplestoreDAO dao = getTriplestoreDAO();
		Subject subject = new Subject(taxonQname);
		if (req.getRequestURI().contains("/hideTaxon/")) {
			dao.store(subject, new Statement(HIDDEN_TAXON, true));
		} else {
			dao.delete(subject, HIDDEN_TAXON);
		}
		
		taxon.invalidateSelf();

		return apiSuccessResponse(res);
	}

}
