package fi.luomus.triplestore.service;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.services.BaseServlet;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.session.SessionHandlerImple;
import fi.luomus.commons.utils.URIBuilder;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.models.CreatableResource;
import fi.luomus.triplestore.models.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.jdbc.pool.DataSource;

public abstract class EditorBaseServlet extends BaseServlet {

	private static final long serialVersionUID = -421783375686196119L;

	public static enum Format { RDFXMLABBREV, RDFXML, JSON, XML, JSONP, JSON_RDFXMLABBREV, JSON_RDFXML }
	public static final Format DEFAULT_FORMAT = Format.RDFXMLABBREV;

	@Override
	protected String configFileName() {
		return "triplestore-v2.properties";
	}

	@Override
	protected void applicationInit() {}

	@Override
	protected void applicationInitOnlyOnce() {}

	@Override
	protected void applicationDestroy() {
		try {
			if (dataSource != null) dataSource.close();
		} catch (Exception e) {}
	}

	@Override
	protected ResponseData notAuthorizedRequest(HttpServletRequest req, HttpServletResponse res) {
		log(req);
		URIBuilder uri = new URIBuilder(getConfig().get("LoginURL"));
		uri.addParameter("originalURL", req.getRequestURL() != null ? req.getRequestURL() : "" );
		return redirectTo(uri.toString(), res);
	}

	protected static final List<CreatableResource> CREATABLE_RESOURCES = Utils.list(
			new CreatableResource("MA", "Person", "MA.person"),
			new CreatableResource("KE", "Information System", "KE.informationSystem"),
			new CreatableResource("ML", "Area", "ML.area"),
			new CreatableResource("MP", "Publication", "MP.publication"));

	protected ResponseData initResponseData(HttpServletRequest req) throws Exception {
		log(req);
		ResponseData responseData = new ResponseData().setDefaultLocale("en");
		SessionHandler session = getSession(req);
		if (session.hasSession() && session.isAuthenticatedFor("triplestore")) {
			responseData.setData("user", getUser(session));
			responseData.setData("flashMessage", session.getFlash());
			responseData.setData("successMessage", session.getFlashSuccess());
			responseData.setData("errorMessage", session.getFlashError());
			responseData.setData("creatableResources", CREATABLE_RESOURCES);
			Config config = getConfig();
			if (config.defines("TriplestoreSelf_Username")) {
				responseData.setData("TriplestoreSelf_Username", config.get("TriplestoreSelf_Username"));
				responseData.setData("TriplestoreSelf_Password", config.get("TriplestoreSelf_Password"));
			}
		}
		Config config = getConfig();
		if (config.defines("taxonomyEditorBaseURL")) {
			responseData.setData("taxonomyEditorBaseURL", config.get("taxonomyEditorBaseURL"));
		}
		return responseData;
	}

	protected User getUser(HttpServletRequest req) {
		return getUser(getSession(req));
	}

	private User getUser(SessionHandler session) {
		User.Role role = User.Role.NORMAL_USER;
		if ("admin".equals(session.get("role"))) {
			role = User.Role.ADMIN;
		}
		return new User(session.userId(), session.get("user_qname"), session.userName(), role);
	}

	@Override
	protected boolean authorized(HttpServletRequest req) {
		SessionHandler session = getSession(req);
		return session.isAuthenticatedFor("triplestore");
	}

	protected TriplestoreDAO getTriplestoreDAO(HttpServletRequest req) throws IllegalAccessException {
		DataSource datasource = getDataSource();
		User user = getUser(req);
		return new TriplestoreDAOImple(datasource, user.getQname());
	}

	protected TriplestoreDAO getTriplestoreDAO() {
		DataSource datasource = getDataSource();
		return new TriplestoreDAOImple(datasource, TriplestoreDAO.SYSTEM_USER);
	}

	private static DataSource dataSource = null;

	private DataSource getDataSource() {
		if (dataSource == null) {
			TriplestoreDAOConst.SCHEMA = getConfig().get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(getConfig().connectionDescription());
		}
		return dataSource;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return redirectTo404(res);
	}

	protected String getQname(HttpServletRequest req) {
		return getId(req);
	}

	protected static boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}


	@Override
	protected SessionHandler getSession(HttpServletRequest req) {
		SessionHandler sessionHandler = new SessionHandlerImple(req.getSession(true), "triplestore");
		return sessionHandler;
	}

	protected ResponseData jsonResponse(String json, HttpServletResponse res) throws Exception {
		res.setContentType("application/json; charset=utf-8");
		PrintWriter out = res.getWriter();
		out.write(json);
		out.flush();
		return new ResponseData().setOutputAlreadyPrinted();
	}

	protected ResponseData rdfResponse(String xml, HttpServletResponse res) throws IOException {
		res.setContentType("application/rdf+xml; charset=utf-8");
		PrintWriter out = res.getWriter();
		out.write(xml);
		out.flush();
		return new ResponseData().setOutputAlreadyPrinted();
	}

	protected static boolean jsonRequest(Format format) {
		return format.toString().startsWith("JSON");
	}

	private static final Map<String, Format> formats;
	static {
		formats = new HashMap<String, Format>();
		for (Format format : Format.values()) {
			formats.put(format.toString().toUpperCase(), format);
		}
	};

	protected Format getFormat(HttpServletRequest req) {
		String format = req.getParameter("format");
		if (format ==  null) return DEFAULT_FORMAT;
		Format f = formats.get(format.toUpperCase());
		if (f != null) return f;
		return DEFAULT_FORMAT;
	}

}
