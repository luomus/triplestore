package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/detachTaxon/*"})
public class ApiDetachTaxon extends ApiBaseServlet {

	private static final long serialVersionUID = 2796206998214459426L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			return redirectTo500(res);
		} 
			
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(new Qname(taxonQname));
		checkPermissionsToAlterTaxon(taxon, req);
		
		if (taxon.hasCriticalData()) {
			throw new IllegalStateException("Can not detach "+taxonQname+": It has critical data.");
		}
		
		taxon.invalidate();
		
		getTriplestoreDAO(req).delete(new Subject(taxonQname), new Predicate("MX.isPartOf"));
		getTriplestoreDAO(req).delete(new Subject(taxonQname), new Predicate("MX.nameAccordingTo"));

		return apiSuccessResponse(res);
	}
	
}
