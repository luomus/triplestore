package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/editors/*"})
public class IUCNEditorsServlet extends IUCNFrontpageServlet {

	private static final Predicate TAXON_GROUP_PREDICATE = new Predicate("MKV.taxonGroup");
	private static final Predicate IUCN_EDITOR_PREDICATE = new Predicate("MKV.iucnEditor");
	private static final long serialVersionUID = 2954366290743144344L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins.");
		ResponseData responseData = initResponseData(req);
		String qname = getQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroups().get(qname);
		if (group == null) {
			return redirectTo404(res);
		}
		TaxonGroupIucnEditors groupEditors = getTaxonomyDAO().getIucnDAO().getGroupEditors().get(group.getQname().toString());
		return responseData.setViewName("iucn-editors")
				.setData("group", group)
				.setData("groupEditors", groupEditors);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (!getUser(req).isAdmin()) throw new IllegalAccessException("Only for admins.");
		String groupQname = getQname(req);
		String groupEditorsQname = req.getParameter("groupEditorsId");
		List<String> editors = editors(req);

		if (given(groupEditorsQname) && editors.isEmpty()) {
			delete(groupEditorsQname);
			getSession(req).setFlashSuccess("Editors removed");
		} else if (editors.isEmpty()){
			getSession(req).setFlashSuccess("Nothing modified");
		} else {
			insertOrUpdate(groupQname, groupEditorsQname, editors);
			getSession(req).setFlashSuccess("Editors saved");
		}
		return redirectTo(getConfig().baseURL()+"/iucn/editors/"+groupQname, res);
	}

	private List<String> editors(HttpServletRequest req) {
		List<String> editors = new ArrayList<>();
		for (String editor : req.getParameterValues("editor")) {
			if (given(editor)) editors.add(editor);
		}
		return editors;
	}

	private void insertOrUpdate(String groupQname, String groupEditorsQname, List<String> editors) throws Exception {
		Model model = getOrInit(groupQname, groupEditorsQname);
		updateEditors(editors, model);

		getTriplestoreDAO().store(model);

		getTaxonomyDAO().getIucnDAO().clearEditorCache();
	}

	private void delete(String groupEditorsQname) throws Exception {
		getTriplestoreDAO().delete(new Subject(groupEditorsQname));
	}

	private void updateEditors(List<String> editors, Model model) {
		model.removeAll(IUCN_EDITOR_PREDICATE);
		for (String editor : editors) {
			if (given(editor)) {
				model.addStatement(new Statement(IUCN_EDITOR_PREDICATE, new ObjectResource(editor)));
			}
		}
	}

	private Model getOrInit(String groupQname, String groupEditorsQname) throws Exception {
		Model model = null;
		if (given(groupEditorsQname)) {
			model = getTriplestoreDAO().get(groupEditorsQname);
		} else {
			model = new Model(getTriplestoreDAO().getSeqNextValAndAddResource(IUCN_NAMESPACE));
			model.setType("MKV.taxonGroupIucnEditors");
			model.addStatement(new Statement(TAXON_GROUP_PREDICATE, new ObjectResource(groupQname)));
		}
		return model;
	}

}
