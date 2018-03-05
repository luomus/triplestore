package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/detachTaxon/*"})
public class ApiDetachTaxon extends ApiBaseServlet {

	private static final Predicate NAME_ACCORDING_TO_PREDICATE = new Predicate("MX.nameAccordingTo");
	private static final Predicate IS_PART_OF_PREDICATE = new Predicate("MX.isPartOf");
	private static final long serialVersionUID = 2796206998214459426L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			return redirectTo500(res);
		} 
		if (!taxonQname.contains(".")) taxonQname = taxonQname.replace("MX", "MX.");
		
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(new Qname(taxonQname));
		checkPermissionsToAlterTaxon(taxon, req);
		
		if (!taxon.isDetachable()) {
			throw new IllegalStateException("Can not detach "+taxonQname+" because of preconditions.");
		}
		
		taxon.invalidateSelfAndLinking();
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		Subject subject = new Subject(taxonQname);
		
		dao.delete(subject, IS_PART_OF_PREDICATE);
		dao.delete(subject, NAME_ACCORDING_TO_PREDICATE);
		
		return apiSuccessResponse(res);
	}

}
