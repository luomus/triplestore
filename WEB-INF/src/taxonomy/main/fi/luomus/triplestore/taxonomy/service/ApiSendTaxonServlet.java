package fi.luomus.triplestore.taxonomy.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/sendtaxon/*"})
public class ApiSendTaxonServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 7714464443111669323L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {

		String taxonToSendID = req.getParameter("taxonToSendID").replace("MX", "MX.");
		String newParentID = req.getParameter("newParentID");

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
		
		toSend.invalidate();

		TriplestoreDAO dao = getTriplestoreDAO(req);
		dao.store(new Subject(taxonToSendID), new Statement(new Predicate("MX.isPartOf"), new ObjectResource(newParentID)));
		dao.store(new Subject(taxonToSendID), new Statement(new Predicate("sortOrder"), new ObjectLiteral(ApiChangeParentServlet.LAST_IN_ORDER)));

		toSend.invalidate();
		newParent.invalidate();

		return apiSuccessResponse(res);
	}

}
