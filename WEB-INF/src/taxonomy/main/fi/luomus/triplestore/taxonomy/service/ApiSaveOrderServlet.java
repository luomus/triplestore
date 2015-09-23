package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/saveorder/*"})
public class ApiSaveOrderServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3916869909638885196L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String order = req.getParameter("order");
		String parent = order.split(",")[0];
		parent = parent.trim().replace("MX", "MX.");
		
		checkPermissionsToAlterTaxon(parent, req);
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		int i = 0;
		for (String taxon : order.split(",")) {
			taxon = taxon.trim().replace("MX", "MX.");
			dao.store(new Subject(taxon), new Statement(new Predicate("sortOrder"), i++));
			getTaxonomyDAO().invalidateTaxon(new Qname(taxon));
		}
		getTaxonomyDAO().invalidateTaxon(new Qname(parent));
		
		return new ResponseData().setOutputAlreadyPrinted();
	}
	
}

