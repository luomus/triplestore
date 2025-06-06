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
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/saveorder/*"})
public class ApiSaveOrderServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3916869909638885196L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String order = req.getParameter("order");
		String firstToOrder = order.split(",")[0];
		firstToOrder = firstToOrder.trim().replace("MX", "MX.");
		
		checkPermissionsToAlterTaxon(firstToOrder, req);
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		
		int i = 0;
		for (String taxon : order.split(",")) {
			taxon = taxon.trim().replace("MX", "MX.");
			dao.store(new Subject(taxon), new Statement(new Predicate("sortOrder"), i++));
			((EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxon))).invalidateSelf();
		}
		
		return apiSuccessResponse(res);
	}
	
}

