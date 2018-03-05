package fi.luomus.triplestore.taxonomy.service;

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
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;
import fi.luomus.triplestore.taxonomy.service.ApiAddSynonymServlet.SynonymType;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/sendTaxon/*"})
public class ApiSendTaxonServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7714464443111669323L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String taxonToSendID = req.getParameter("taxonToSendID");
		String newParentID = req.getParameter("newParentID");
		if (!taxonToSendID.contains("MX.")) taxonToSendID = taxonToSendID.replace("MX", "MX.");
		if (!newParentID.contains("MX.")) newParentID = newParentID.replace("MX", "MX.");
		String sendAsType = req.getParameter("sendAsType");

		Utils.debug(taxonToSendID, newParentID, sendAsType);

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

		TriplestoreDAO dao = getTriplestoreDAO(req);

		toSend.invalidateSelfAndLinking();

		removeExistingLinkings(toSend, newParent, dao);
		if ("CHILD".equals(sendAsType)) {
			ApiChangeParentServlet.move(toSend, newParent, dao);
		} else {
			SynonymType synonymType = ApiAddSynonymServlet.getSynonymType(sendAsType);
			moveAsSynonym(toSend, newParent, synonymType, dao);
		}

		toSend = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonToSendID));
		toSend.invalidateSelfAndLinking();

		return apiSuccessResponse(res);
	}

	private void removeExistingLinkings(EditableTaxon toSend, EditableTaxon newParent, TriplestoreDAO dao) throws Exception {
		Taxon synonymParent = toSend.getSynonymParent();
		if (synonymParent != null) {
			Model synonymParentModel = dao.get(synonymParent.getQname());
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

	private void moveAsSynonym(EditableTaxon toSend, EditableTaxon newParent, SynonymType synonymType, TriplestoreDAO dao) throws Exception {
		dao.delete(new Subject(toSend.getQname()), ApiChangeParentServlet.NAME_ACCORDING_TO_PREDICATE);
		dao.insert(new Subject(newParent.getQname()), new Statement(ApiAddSynonymServlet.getPredicate(synonymType), new ObjectResource(toSend.getQname())));
	}

}
