package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/group/*"})
public class IUCNGroupSpeciesListServlet extends IUCNFrontpageServlet {

	private static final long serialVersionUID = -9070472068743470346L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String qname = getQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroups().get(qname);
		if (group == null) {
			return redirectTo404(res);
		}

		TaxonGroupIucnEditors groupEditors = getGroupEditors().get(group.getQname().toString());

		return responseData.setViewName("iucn-group-species-list")
				.setData("group", group)
				.setData("groupEditors", groupEditors);
	}

}
