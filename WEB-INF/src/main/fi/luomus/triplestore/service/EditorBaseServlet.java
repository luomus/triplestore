package fi.luomus.triplestore.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.BaseServlet;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.session.SessionHandlerImple;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.URIBuilder;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.models.CreatableResource;
import fi.luomus.triplestore.models.User;

public abstract class EditorBaseServlet extends BaseServlet {

	private static final long serialVersionUID = -421783375686196119L;

	public static enum Format { RDFXMLABBREV, RDFXML, JSON, XML, JSONP, JSON_RDFXMLABBREV, JSON_RDFXML }
	public static final Format DEFAULT_FORMAT = Format.RDFXMLABBREV;
	private static final Object LOCK = new Object();

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
		String originalURL = getURL(req);
		String next = originalURL.replace("/triplestore", "").replace("/taxonomy-editor", "");
		uri.addParameter("next", next);
		return redirectTo(uri.toString());
	}

	public static String getURL(HttpServletRequest req) {
		String servletPath = req.getServletPath();   // /servlet/MyServlet
		String pathInfo = req.getPathInfo();         // /a/b;c=123
		String queryString = req.getQueryString();   // d=789

		// Reconstruct original requesting URL
		StringBuilder url = new StringBuilder();
		url.append(servletPath);
		if (pathInfo != null) {
			url.append(pathInfo);
		}
		if (queryString != null) {
			url.append("?").append(queryString);
		}
		return url.toString();
	}

	protected static final List<CreatableResource> CREATABLE_RESOURCES = Utils.list(
			new CreatableResource("MA", "Person", "MA.person"),
			new CreatableResource("KE", "Information System", "KE.informationSystem"),
			new CreatableResource("ML", "Area", "ML.area"),
			new CreatableResource("MP", "Publication", "MP.publication"));

	private static final Date restartDate = new Date();
	private static final Date lastRestartNofity = initLastRestartNofify();
	private static final Set<Qname> restartNotified = new HashSet<>();

	public static String getRestartMessage(User user) {
		if (user == null) return null;
		if (user.getQname() == null || !user.getQname().isSet()) return null;
		if (restartNotified.contains(user.getQname())) return null;
		restartNotified.add(user.getQname());
		if (!restartedLately()) return null;
		return "Käynnistetty uudelleen / editor was restarted at " + DateUtils.format(restartDate, "dd.MM.yyyy HH:mm") + ". Pahoittelut häiriöstä! Apologies for the for the inconvenience!";
	}

	private static Date initLastRestartNofify() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, 30);
		return c.getTime();
	}

	private static boolean restartedLately() {
		return new Date().before(lastRestartNofity);
	}

	protected ResponseData initResponseData(HttpServletRequest req, boolean requireBaseData) throws Exception {
		ResponseData responseData = new ResponseData().setDefaultLocale("en");
		SessionHandler session = getSession(req);
		Config config = getConfig();

		if (session.hasSession() && session.isAuthenticatedFor(config.systemId())) {
			User user = getUser(session);
			responseData.setData("user", user);
			responseData.setData("flashMessage", session.getFlash());
			responseData.setData("successMessage", session.getFlashSuccess());
			responseData.setData("errorMessage", session.getFlashError());
			responseData.setData("restartMessage", getRestartMessage(user));
			responseData.setData("creatableResources", CREATABLE_RESOURCES);
			if (requireBaseData) {
				responseData.setData("resources", getTriplestoreDAO().getResourceStats());
			}
			if (config.defines("TriplestoreSelf_Username")) {
				responseData.setData("TriplestoreSelf_Username", config.get("TriplestoreSelf_Username"));
				responseData.setData("TriplestoreSelf_Password", config.get("TriplestoreSelf_Password"));
			}
		}

		if (config.defines("taxonomyEditorBaseURL")) {
			responseData.setData("taxonomyEditorBaseURL", config.get("taxonomyEditorBaseURL"));
			responseData.setData("lajiETLBaseURL", config.get("lajiETLBaseURL"));
		}

		return responseData;
	}

	protected User getUser(HttpServletRequest req) {
		return getUser(getSession(req));
	}

	protected User getUser(SessionHandler session) {
		User.Role role = getRole(session);
		return new User(new Qname(session.get("user_qname")), session.get("person_token"), session.userName(), role);
	}

	private User.Role getRole(SessionHandler session) {
		User.Role role = User.Role.NORMAL_USER;
		Set<String> roles = getRoles(session);
		if (roles.contains("MA.admin")) {
			role = User.Role.ADMIN;
		} else if (roles.contains("MA.taxonEditorUserDescriptionWriterOnly")) {
			role = User.Role.DESCRIPTION_WRITER;
		}
		return role;
	}

	@SuppressWarnings("unchecked")
	private Set<String> getRoles(SessionHandler session) {
		Set<String> roles =  (Set<String>) session.getObject("roles");
		if (roles == null || roles.isEmpty()) {
			session.invalidate();
			throw new IllegalStateException();
		}
		return roles ;
	}

	@Override
	protected boolean authorized(HttpServletRequest req) {
		SessionHandler session = getSession(req);
		return session.isAuthenticatedFor(getConfig().systemId());
	}

	protected TriplestoreDAO getTriplestoreDAO(HttpServletRequest req) {
		DataSource datasource = getDataSource();
		User user = getUser(req);
		return new TriplestoreDAOImple(datasource, user.getQname(), getErrorReporter());
	}

	protected TriplestoreDAO getTriplestoreDAO() {
		DataSource datasource = getDataSource();
		return new TriplestoreDAOImple(datasource, TriplestoreDAO.SYSTEM_USER, getErrorReporter());
	}

	private static DataSource dataSource = null;

	private DataSource getDataSource() {
		if (dataSource == null) {
			synchronized (LOCK) {
				if (dataSource == null) {
					TriplestoreDAOConst.SCHEMA = getConfig().get("LuontoDbName");
					dataSource = DataSourceDefinition.initDataSource(getConfig().connectionDescription());
				}
			}
		}
		return dataSource;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return status404(res);
	}

	protected String getQname(HttpServletRequest req) {
		return getId(req);
	}

	protected static boolean given(Qname qname) {
		return qname != null && qname.isSet();
	}

	protected static boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	protected static boolean given(Object ... objects) {
		for (Object o : objects) {
			if (!given(o)) return false;
		}
		return true;
	}

	@Override
	protected SessionHandler getSession(HttpServletRequest req) {
		SessionHandler sessionHandler = new SessionHandlerImple(req.getSession(true), getConfig().systemId());
		return sessionHandler;
	}

	protected ResponseData rdfResponse(String xml) {
		return response(xml, "application/rdf+xml");
	}

	protected static boolean jsonRequest(Format format) {
		return format.toString().startsWith("JSON");
	}

	private static final Map<String, Format> formats;
	static {
		formats = new HashMap<>();
		for (Format format : Format.values()) {
			formats.put(format.toString().toUpperCase(), format);
		}
	}

	protected Format getFormat(HttpServletRequest req) {
		String format = req.getParameter("format");
		if (format ==  null) return DEFAULT_FORMAT;
		Format f = formats.get(format.toUpperCase());
		if (f != null) return f;
		return DEFAULT_FORMAT;
	}

}
