package fi.luomus.triplestore.taxonomy.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Content.Context;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/taxon-descriptions/*"})
public class TaxonDescriptionsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -2281661002076649983L;

	public static final Set<User.Role> ALLOWED = Collections.unmodifiableSet(Utils.set(User.Role.ADMIN, User.Role.NORMAL_USER, User.Role.DESCRIPTION_WRITER));

	@Override
	protected Set<User.Role> allowedRoles() {
		return ALLOWED;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req);
		Qname taxonQname = new Qname(getQname(req));
		if (!taxonQname.isSet()) {
			taxonQname = TaxonomyTreesEditorServlet.DEFAULT_ROOT_QNAME;
		}

		TaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		if (!taxonomyDAO.getTaxonContainer().hasTaxon(taxonQname)) {
			return status404(res);
		}

		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);

		Map<String, List<RdfProperty>> descriptionGroupVariables = getTriplestoreDAO().getDescriptionGroupVariables();
		Set<String> groupsWithContent = resolveGroupsWithContent(descriptionGroupVariables, taxon);

		if (taxon.getChecklist() != null) {
			responseData.setData("checklist", taxonomyDAO.getChecklists().get(taxon.getChecklist().toString()));
		}

		return responseData
				.setViewName("taxonDescriptions")
				.setData("taxon", taxon)
				.setData("root", taxon)
				.setData("variables", descriptionGroupVariables)
				.setData("groups", getTriplestoreDAO().getDescriptionGroups())
				.setData("groupsWithContent", groupsWithContent);
	}

	private Set<String> resolveGroupsWithContent(Map<String, List<RdfProperty>> descriptionGroupVariables, Taxon taxon) {
		Set<String> groupsWithContent = new HashSet<>();
		for (Map.Entry<String, List<RdfProperty>> e : descriptionGroupVariables.entrySet()) {
			String group = e.getKey();
			List<RdfProperty> descriptionVariables = e.getValue(); 
			if (hasContent(taxon, descriptionVariables)) {
				groupsWithContent.add(group);
			}
		}
		return groupsWithContent;
	}

	private boolean hasContent(Taxon taxon, List<RdfProperty> descriptionVariables) {
		Context descriptions = taxon.getDescriptions().getDefaultContext();
		if (descriptions == null) return false;

		for (RdfProperty descriptionVariable : descriptionVariables) {
			String property = descriptionVariable.getQname().toString();
			for (String locale : ApiTaxonEditSectionSubmitServlet.SUPPORTED_LANGUAGES) {
				if (given(descriptions.getText(property, locale))) return true;
			}
		}
		return false;
	}

}
