package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/deleteTaxon/*"})
public class ApiDeleteTaxon extends ApiBaseServlet {

	private static final long serialVersionUID = 2796206998214459426L;

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

		if (!taxon.isDeletable()) {
			throw new IllegalStateException("Can not delete taxon "+taxonQname+" because of preconditions");
		}

		taxon.invalidateSelfAndLinking();

		TriplestoreDAO dao = getTriplestoreDAO(req);

		delete(taxon, dao);

		return apiSuccessResponse(res);
	}

	private void delete(EditableTaxon taxon, TriplestoreDAO dao) throws Exception {
		if (taxon.isSynonym()) {
			unlinkSynonym(taxon, dao);
		}

		dao.delete(new Subject(taxon.getQname()));
	}

	private void unlinkSynonym(EditableTaxon removedTaxon, TriplestoreDAO dao) throws Exception {
		String removedId = removedTaxon.getQname().toString();
		Qname synonymParentId = getTaxonomyDAO().getTaxonContainer().getSynonymParent(removedTaxon.getQname());
		if (synonymParentId == null) return;
		
		Model synonymParentModel = dao.get(synonymParentId);
		for (Statement s : synonymParentModel.getStatements()) {
			if (shouldDelete(removedId, s)) {
				dao.deleteStatement(s.getId());
			}
		}
	}

	private boolean shouldDelete(String removedId, Statement s) {
		return s.isResourceStatement() && s.getObjectResource().getQname().equals(removedId) && isSynonymPredicate(s.getPredicate());
	}

	private boolean isSynonymPredicate(Predicate predicate) {
		return ApiAddSynonymServlet.SYNONYM_PREDICATES.values().contains(predicate);
	}

}
