package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.IUCNYearlyGroupStat;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-stat/*"})
public class ApiIucnGroupStatServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3859060737786587345L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String groupQname = getId(req);
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(groupQname)) return redirectTo404(res);

		IUCNYearlyGroupStat stat = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getStat(year, groupQname);

		return new ResponseData().setViewName("iucn-stat").setData("stat", stat);
	}

}
