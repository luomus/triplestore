package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/deleteTaxon/*"})
public class ApiDeleteTaxon extends ApiBaseServlet {

	private static final long serialVersionUID = 2796206998214459426L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			return redirectTo500(res);
		} 
		
		EditableTaxon taxon = (EditableTaxon) getTaxonomyDAO().getTaxon(new Qname(taxonQname));
		checkPermissionsToAlterTaxon(taxon, req);
		
		int created = taxon.getCreatedAtTimestamp();
		long lastAllowed = getLastAllowedTaxonDeleteTimestamp();
		
		if (created < lastAllowed) {
			throw new IllegalStateException("Can no longer delete taxon "+taxonQname+" because time limit has gone: " + created + " >= " + lastAllowed );
		}
		if (taxon.hasChildren()) {
			throw new IllegalStateException("Can not delete "+taxonQname+": It has children.");
		}
		
		taxon.invalidate();
		
		getTriplestoreDAO(req).delete(new Subject(taxonQname));
		
		return apiSuccessResponse(res);
	}
	
}
