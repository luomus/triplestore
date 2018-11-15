package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/publications/*", "/taxonomy-editor/publications/add/*"})
public class PublicationsServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 282606927755194059L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		if (req.getRequestURI().endsWith("/publications")) {
			responseData.setData("publications", getTaxonomyDAO().getPublicationsForceReload());
			return responseData.setViewName("publications");	
		}
		if (addNew(req)) {
			return responseData.setViewName("publications-edit").setData("action", "add").setData("publication", new Publication(null));
		}
		String qname = getQname(req);
		Publication publication = getTaxonomyDAO().getPublicationsForceReload().get(qname);
		if (publication == null) {
			return status404(res);
		}
		return responseData.setViewName("publications-edit").setData("action", "modify").setData("publication", publication);
	}

	private boolean addNew(HttpServletRequest req) {
		return req.getRequestURI().endsWith("/add");
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		boolean addNew = addNew(req);
		TriplestoreDAO triplestoreDAO = getTriplestoreDAO(req);
		Qname qname = addNew ? null : new Qname(getQname(req));
		String citation = req.getParameter("citation");
		String uri = req.getParameter("URI");

		Publication publication = new Publication(qname);
		publication.setCitation(citation);
		publication.setURI(uri);

		triplestoreDAO.storePublication(publication);
		getTaxonomyDAO().getPublicationsForceReload();
		
		if (addNew) {
			getSession(req).setFlashSuccess("New publication added");
			return redirectTo(getConfig().baseURL()+"/publications");
		} else {
			getSession(req).setFlashSuccess("Publication modified");
			return redirectTo(getConfig().baseURL()+"/publications/"+qname);
		}
	}

}
