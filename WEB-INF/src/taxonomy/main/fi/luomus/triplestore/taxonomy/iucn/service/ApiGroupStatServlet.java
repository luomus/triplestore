package fi.luomus.triplestore.taxonomy.iucn.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.iucn.model.YearlyGroupStat;
import fi.luomus.triplestore.taxonomy.service.ApiBaseServlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-stat/*"})
public class ApiGroupStatServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3859060737786587345L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String groupQname = getId(req);
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(groupQname)) return status404(res);

		YearlyGroupStat stat = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getStat(year, groupQname);

		return new ResponseData().setViewName("iucn-stat").setData("stat", stat);
	}

}
