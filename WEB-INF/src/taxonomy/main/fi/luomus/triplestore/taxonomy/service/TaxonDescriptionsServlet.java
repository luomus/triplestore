package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			return redirectTo500(res);
		} 

		TriplestoreDAO dao = getTriplestoreDAO();
		Map<String, List<RdfProperty>> descriptionGroupVariables = new LinkedHashMap<>();
		List<RdfProperty> descriptionGroups = dao.getAltValues(SPECIES_DESC_VARIABLES);
		
		for (RdfProperty descriptionGroup : descriptionGroups) {
			descriptionGroupVariables.put(descriptionGroup.getQname().toString(), dao.getAltValues(descriptionGroup.getQname()));
		}
		
		Taxon taxon = getTaxonomyDAO().getTaxon(new Qname(taxonQname));

		return responseData.setViewName("taxon-descriptions").setData("taxon", taxon).setData("root", taxon).setData("variables", descriptionGroupVariables).setData("groups", descriptionGroups);
	}

}
