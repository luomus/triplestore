package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/detachTaxon/*"})
public class ApiDetachTaxon extends ApiBaseServlet {

	private static final Predicate CIRCUMSCRIPTION_PREDICATE = new Predicate("MX.circumscription");
	private static final Predicate NAME_ACCORDING_TO_PREDICATE = new Predicate("MX.nameAccordingTo");
	private static final Predicate IS_PART_OF_PREDICATE = new Predicate("MX.isPartOf");
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
		
		boolean hasSynonyms = !taxon.getSynonyms().isEmpty();
		
		taxon.invalidate();
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		Subject subject = new Subject(taxonQname);
		
		dao.delete(subject, IS_PART_OF_PREDICATE);
		dao.delete(subject, NAME_ACCORDING_TO_PREDICATE);
		if (hasSynonyms) {
			removeFromConcept(taxon, dao);
		}

		taxon.invalidate();
		
		return apiSuccessResponse(res);
	}

	private void removeFromConcept(EditableTaxon taxon, TriplestoreDAO dao) throws Exception {
		Qname concept = dao.getSeqNextValAndAddResource("MC");
		taxon.setTaxonConceptQname(concept);
		dao.store(new Subject(taxon.getQname()), new Statement(CIRCUMSCRIPTION_PREDICATE, new ObjectResource(concept)));
	}
	
}
