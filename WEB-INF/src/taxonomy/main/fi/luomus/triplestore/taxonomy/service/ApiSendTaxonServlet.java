package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.service.ApiAddSynonymServlet.SynonymType;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/sendTaxon/*"})
public class ApiSendTaxonServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7714464443111669323L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String taxonToSendID = req.getParameter("taxonToSendID");
		String newParentID = req.getParameter("newParentID");
		if (!taxonToSendID.contains("MX.")) taxonToSendID = taxonToSendID.replace("MX", "MX.");
		if (!newParentID.contains("MX.")) newParentID = newParentID.replace("MX", "MX.");
		String sendAsType = req.getParameter("sendAsType");

		notNull(taxonToSendID, newParentID, sendAsType);
		if(taxonToSendID.equals(newParentID)) {
			return apiErrorResponse("Sending taxon as child of itself is not allowed", res);
		}

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon toSend = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonToSendID));
		EditableTaxon newParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newParentID));

		boolean permissionsToEither = false;
		try {
			checkPermissionsToAlterTaxon(toSend, req);
			permissionsToEither = true;
		} catch (Exception e) {}
		if (!permissionsToEither) {
			try {
				checkPermissionsToAlterTaxon(newParent, req);
				permissionsToEither = true;
			} catch (Exception e) {}
		}
		if (!permissionsToEither) {
			return apiErrorResponse("No permissions to move the taxon. You should have permissions either to the taxon being moved OR the new parent.", res);
		}
		if (!given(newParent.getChecklist())) {
			return apiErrorResponse("Sending taxon to non-checklist taxon is not allowed", res);
		} else if (given(toSend.getChecklist())) {
			if (!toSend.getChecklist().equals(newParent.getChecklist())) {
				return apiErrorResponse("Can not send a checklist taxon into other checklist", res);
			}
		}
		if (moveAsChild(sendAsType)) {
			if (!newParent.getChecklist().equals(toSend.getChecklist())) {
				return apiErrorResponse("Moving as child is allowed only within the same checklist", res);
			}
		}

		TriplestoreDAO dao = getTriplestoreDAO(req);

		toSend.invalidateSelfAndLinking();

		removeExistingLinkings(toSend, dao);
		if (moveAsChild(sendAsType)) {
			ApiChangeParentServlet.move(toSend, newParent, dao);
			toSend = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonToSendID));
			toSend.invalidateSelfAndLinking();
		} else {
			SynonymType synonymType = ApiAddSynonymServlet.getSynonymType(sendAsType);
			moveAsSynonym(toSend, newParent, synonymType, dao);
			newParent.invalidateSelf();
		}

		return apiSuccessResponse(res);
	}


	private boolean moveAsChild(String sendAsType) {
		return "CHILD".equals(sendAsType);
	}


	private void notNull(String ... strings) {
		for (String s : strings) {
			if (!given(s)) throw new IllegalArgumentException("Empty given");
		}
	}

	private void removeExistingLinkings(EditableTaxon toSend, TriplestoreDAO dao) throws Exception {
		Qname synonymParentId = getTaxonomyDAO().getTaxonContainer().getSynonymParent(toSend.getQname());
		if (synonymParentId != null) {
			Model synonymParentModel = dao.get(synonymParentId);
			for (Predicate synonymPredicate : ApiAddSynonymServlet.SYNONYM_PREDICATES.values()) {
				for (Statement s : synonymParentModel.getStatements(synonymPredicate.getQname())) {
					if (s.isResourceStatement() && s.getObjectResource().getQname().equals(toSend.getQname().toString())) {
						dao.deleteStatement(s.getId());
					}
				}
			}
		}
		dao.delete(new Subject(toSend.getQname()), ApiChangeParentServlet.IS_PART_OF_PREDICATE);
	}

	private void moveAsSynonym(EditableTaxon toSend, EditableTaxon newSynonymParent, SynonymType synonymType, TriplestoreDAO dao) throws Exception {
		deleteNonCriticalIucnEvaluations(toSend);
		Subject taxonToSendId = new Subject(toSend.getQname());
		Subject newSynonymParentId = new Subject(newSynonymParent.getQname()); 
		dao.delete(taxonToSendId, new Predicate("MX.taxonExpert"));
		dao.delete(taxonToSendId, new Predicate("MX.taxonEditor"));
		dao.delete(taxonToSendId, ApiChangeParentServlet.NAME_ACCORDING_TO_PREDICATE);
		dao.insert(newSynonymParentId, new Statement(ApiAddSynonymServlet.getPredicate(synonymType), new ObjectResource(toSend.getQname())));
	}

	private void deleteNonCriticalIucnEvaluations(EditableTaxon toSend) throws Exception {
		String taxonId = toSend.getQname().toString();
		deleteNonCriticalIucnEvaluations(taxonId);
	}

	private void deleteNonCriticalIucnEvaluations(String taxonId) throws Exception {
		IucnDAO iucnDAO = getTaxonomyDAO().getIucnDAO();
		Container iucnContainer = iucnDAO.getIUCNContainer();
		if (iucnContainer.hasTarget(taxonId)) {
			for (Evaluation e : new ArrayList<>(iucnContainer.getTarget(taxonId).getEvaluations())) {
				if (e.isCriticalDataEvaluation()) {
					throw new IllegalStateException("Critical data validation should prevent this. Deleting evaluation with status " + e.getIucnStatus());
				}
				iucnDAO.deleteEvaluation(taxonId, e.getEvaluationYear());
			}
		}
	}

}
