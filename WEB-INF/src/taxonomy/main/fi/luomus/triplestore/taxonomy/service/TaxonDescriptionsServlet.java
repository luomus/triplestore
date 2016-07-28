package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.Content.Context;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/taxon-descriptions/*"})
public class TaxonDescriptionsServlet extends TaxonomyEditorBaseServlet {

	private static final Qname SPECIES_DESC_VARIABLES = new Qname("MX.speciesDescriptionVariables");
	private static final long serialVersionUID = -2281661002076649983L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		String taxonQname = getQname(req);
		if (!given(taxonQname)) {
			taxonQname = TaxonomyTreesEditorServlet.DEFAULT_ROOT_QNAME.toString();
		} 

		TriplestoreDAO dao = getTriplestoreDAO();
		Map<String, List<RdfProperty>> descriptionGroupVariables = new LinkedHashMap<>();
		List<RdfProperty> descriptionGroups = dao.getAltValues(SPECIES_DESC_VARIABLES);
		for (RdfProperty descriptionGroup : descriptionGroups) {
			descriptionGroupVariables.put(descriptionGroup.getQname().toString(), dao.getAltValues(descriptionGroup.getQname()));
		}

		TaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonQname));

		Set<String> groupsWithContent = resolveGroupsWithContent(descriptionGroupVariables, taxon);

		if (taxon.getChecklist() != null) {
			responseData.setData("checklist", taxonomyDAO.getChecklists().get(taxon.getChecklist().toString()));
		}

		try {
			checkPermissionsToAlterTaxon(taxon, req);
		} catch (IllegalAccessException noAcess) {
			responseData.setData("noPermissions", "true");
		}
		
		return responseData
				.setViewName("taxonDescriptions")
				.setData("taxon", taxon)
				.setData("root", taxon)
				.setData("variables", descriptionGroupVariables)
				.setData("groups", descriptionGroups)
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
		Context defaultContext = taxon.getDescriptions().getDefaultContext();
		if (defaultContext == null) return false;
		
		for (RdfProperty descriptionVariable : descriptionVariables) {
			String property = descriptionVariable.getQname().toString();
			if (given(defaultContext.getText(property, "fi"))) {
				return true;
			} else if (given(defaultContext.getText(property, "sv"))) {
				return true;
			} else if (given(defaultContext.getText(property, "en"))) {
				return true;
			}
		}
		return false;
	}

}
