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

@WebServlet(urlPatterns = {"/taxonomy-editor/api/changeparent/*"})
public class ApiChangeParentServlet extends ApiBaseServlet {

	private static final long serialVersionUID = 9165675894617551000L;

	public static final String LAST_IN_ORDER = String.valueOf(Integer.MAX_VALUE);

	public static final Predicate IS_PART_OF_PREDICATE = new Predicate("MX.isPartOf");
	public static final Predicate SORT_ORDER_PREDICATE = new Predicate("sortOrder");
	public static final Predicate NAME_ACCORDING_TO_PREDICATE = new Predicate("MX.nameAccordingTo");
	private static final Predicate SCIENTITIF_NAME_PREDICATE = new Predicate("MX.scientificName");
	private static final Qname GENUS = new Qname("MX.genus");

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String taxonQname = req.getParameter("taxon").replace("MX", "MX.");
		String newParentQname = req.getParameter("newParent").replace("MX", "MX.");

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		EditableTaxon oldParent = taxon.hasParent() ? (EditableTaxon) taxonomyDAO.getTaxon(taxon.getParentQname()) : null;
		EditableTaxon newParent = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newParentQname));

		try {
			checkPermissionsToAlterTaxon(taxon, req);
			checkPermissionsToAlterTaxon(newParent, req);
			if (oldParent != null) {
				checkPermissionsToAlterTaxon(oldParent, req);
			}
		} catch (IllegalAccessException noAccess) {
			return new ResponseData().setData("error", noAccess.getMessage()).setViewName("api-error");

		}

		if (taxon.getChecklist() != null && !taxon.getChecklist().equals(newParent.getChecklist())) {
			throw new IllegalAccessException("Can not move a taxon from some other checklist to be a child of another checklist");
		}

		taxon.invalidateSelfAndLinking();
		
		TriplestoreDAO dao = getTriplestoreDAO(req);
		move(taxon, newParent, dao);
		if (taxon.isSpecies()) {
			changeScientificNameAndCreateSynonymOfOldName(taxon, newParent, taxonomyDAO, dao);
		}
		
		taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));
		taxon.invalidateSelfAndLinking();
		
		return apiSuccessResponse(res);
	}

	public static void move(EditableTaxon taxon, EditableTaxon newParent, TriplestoreDAO dao) throws Exception {
		Subject subject = new Subject(taxon.getQname());
		dao.store(subject, new Statement(IS_PART_OF_PREDICATE, new ObjectResource(newParent.getQname())));
		dao.store(subject, new Statement(SORT_ORDER_PREDICATE, new ObjectLiteral(LAST_IN_ORDER)));
		if (!given(taxon.getChecklist())) {
			dao.store(subject, new Statement(NAME_ACCORDING_TO_PREDICATE, new ObjectResource(newParent.getChecklist())));
		}
	}
	
	private void changeScientificNameAndCreateSynonymOfOldName(EditableTaxon taxon, EditableTaxon newParent, ExtendedTaxonomyDAO taxonomyDAO, TriplestoreDAO dao) throws Exception {
		String newParentGenus = newParent.getScientificNameOfRank(GENUS);
		if (!given(newParentGenus)) return;
		String newScientificName = changeGenus(newParentGenus, taxon.getScientificName());
		if (!given(newScientificName)) return;
		if (newScientificName.equals(taxon.getScientificName())) return;

		dao.store(new Subject(taxon.getQname()), new Statement(SCIENTITIF_NAME_PREDICATE, new ObjectLiteral(newScientificName)));
		ApiTaxonEditSectionSubmitServlet.createAndStoreSynonym(dao, taxonomyDAO, taxon);
	}

	public static String changeGenus(String genusName, String scientificName) {
		if (!given(scientificName)) return "";
		if (!scientificName.contains(" ")) return scientificName;
		String speciesEpithet = scientificName.substring(scientificName.indexOf(" "));
		return genusName + speciesEpithet;
	}

}
