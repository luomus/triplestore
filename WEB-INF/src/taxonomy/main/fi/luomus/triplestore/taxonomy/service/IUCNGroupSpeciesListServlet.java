package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/group/*"})
public class IUCNGroupSpeciesListServlet extends IUCNFrontpageServlet {

	private static final long serialVersionUID = -9070472068743470346L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String groupQname = groupQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroups().get(groupQname);
		if (group == null) {
			return redirectTo404(res);
		}

		List<Qname> groupEditors = getTaxonomyDAO().getIucnDAO().getGroupEditors().get(groupQname);
		List<IUCNEvaluationTarget> targets = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getTargetsOfGroup(groupQname);

		return responseData.setViewName("iucn-group-species-list")
				.setData("group", group)
				.setData("groupEditors", groupEditors)
				.setData("targets", targets)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate("MKV.redListStatus")))
				.setData("persons", getTaxonomyDAO().getPersons());
	}

	private String groupQname(HttpServletRequest req) {
		String groupQname = req.getRequestURI().split(Pattern.quote("/group/"))[1].split(Pattern.quote("/"))[0];
		return groupQname;
	}

}
