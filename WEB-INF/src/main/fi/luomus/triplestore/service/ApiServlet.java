package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;

import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.InternalModelToJenaModelConverter;
import fi.luomus.commons.containers.rdf.JenaUtils;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TooManyResultsException;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;
import fi.luomus.triplestore.utils.ConnectionLimiter;
import fi.luomus.triplestore.utils.ConnectionLimiter.Access;

@WebServlet(urlPatterns = {"/*"})
public class ApiServlet extends EditorBaseServlet {

	private static final long serialVersionUID = -1697198692074454503L;

	private static final ConnectionLimiter limiter = new ConnectionLimiter(30);

	protected ConnectionLimiter getConnectionLimiter() {
		return limiter;
	}

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Set<Qname> qnames = new HashSet<>(getQnames(req));
		if (qnames.isEmpty()) {
			return redirectTo404(res);
		}

		Access access = getConnectionLimiter().delayAccessIfNecessary(req.getRemoteUser());
		try {
			return processGetWithAccess(req, res, qnames);
		} finally {
			access.release();
		}
	}

	private ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res, Set<Qname> qnames) throws Exception, IOException {
		Format format = getFormat(req);
		ResultType resultType = getResultType(req);
		TriplestoreDAO dao = getTriplestoreDAO();

		String response = null;
		try {
			response = get(qnames, resultType, format, dao);
		} catch (TooManyResultsException e) {
			return redirectTo403(res);
		}

		if (response == null) {
			return redirectTo404(res);
		}

		if (jsonRequest(format)) {
			return jsonResponse(response, res);
		} else {
			return rdfResponse(response, res);
		}
	}

	public static String get(Qname qname, ResultType resultType, Format format, TriplestoreDAO dao) throws Exception {
		if (qname == null || !qname.isSet()) {
			return null;
		}
		return get(Utils.set(qname), resultType, format, dao);
	}

	public static String get(Set<Qname> qnames, ResultType resultType, Format format, TriplestoreDAO dao) throws Exception {
		if (qnames.isEmpty()) {
			return null;
		}

		String rdf = null;
		if (resultType == ResultType.NORMAL) {
			rdf = normalResultTypeRDF(qnames, format, dao);
		} else {
			rdf = specialResultTypeRDF(qnames, resultType, format, dao);
		}

		if (rdf == null) {
			return null;
		}

		if (jsonRequest(format)) {
			JSONObject jsonObject = XML.toJSONObject(rdf);
			String json = jsonObject.toString();
			return json;
		} else {
			return rdf;
		}
	}

	private static String specialResultTypeRDF(Set<Qname> qnames, ResultType resultType, Format format, TriplestoreDAO dao) throws TooManyResultsException, Exception {
		Collection<Model> models = dao.getSearchDAO().get(qnames, resultType);
		if (models.isEmpty()) {
			return null;
		}
		return generateRdf(models, format);
	}

	private static String normalResultTypeRDF(Set<Qname> qnames, Format format, TriplestoreDAO dao) throws Exception {
		Collection<Model> models = dao.getSearchDAO().get(qnames);
		if (models.isEmpty()) return null;
		return generateRdf(models, format);
	}

	protected static String generateRdf(Collection<Model> models, Format format) {
		String language = FORMAT_TO_RDF_LANG_MAPPING.get(format);
		if (language == null) throw new UnsupportedOperationException("Unknown language for " + format);
		com.hp.hpl.jena.rdf.model.Model jenaModel = new InternalModelToJenaModelConverter(models).getJenaModel();
		String rdfXml = JenaUtils.getRdf(jenaModel, language);
		return rdfXml;
	}

	private static final Map<Format, String> FORMAT_TO_RDF_LANG_MAPPING; 
	static {
		FORMAT_TO_RDF_LANG_MAPPING = new HashMap<Format, String>();
		for (Format format : Format.values()) {
			if (format == Format.JSONP) continue;
			if (format == Format.JSON) {
				FORMAT_TO_RDF_LANG_MAPPING.put(format, "RDF/XML-ABBREV");
				continue;
			}
			if (format.toString().endsWith("ABBREV")) {
				FORMAT_TO_RDF_LANG_MAPPING.put(format, "RDF/XML-ABBREV");
			} else {
				FORMAT_TO_RDF_LANG_MAPPING.put(format, "RDF/XML");
			}
		}
	}

	private static final Map<String, ResultType> RESULT_TYPE_MAPPING;
	static {
		RESULT_TYPE_MAPPING = new HashMap<String, ResultType>();
		for (ResultType resultType : ResultType.values()) {
			RESULT_TYPE_MAPPING.put(resultType.toString().toUpperCase(), resultType);
		}
	};

	private ResultType getResultType(HttpServletRequest req) {
		String resultType = req.getParameter("resulttype");
		if (resultType ==  null) return ResultType.NORMAL;
		ResultType t = RESULT_TYPE_MAPPING.get(resultType.toUpperCase());
		if (t != null) return t;
		return ResultType.NORMAL;
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname qname = new Qname(getQname(req));
		if (!qname.isSet()) {
			return redirectTo500(res);
		}
		Format format = getFormat(req);

		String response = delete(qname, format, getTriplestoreDAO());

		if (jsonRequest(format)) {
			return jsonResponse(response, res);
		} else {
			return rdfResponse(response, res);
		}
	}

	public static String delete(Qname qname, Format format, TriplestoreDAO dao) throws Exception {
		dao.delete(new Subject(qname));

		String rdf = generateRdf(new Model(qname), format);

		if (jsonRequest(format)) {
			JSONObject jsonObject = XML.toJSONObject(rdf);
			String json = jsonObject.toString();
			return json;
		} else {
			return rdf;
		}
	}

	private static String generateRdf(Model model, Format format) {
		return generateRdf(Utils.list(model), format);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return processPut(req, res);
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname qname = new Qname(getQname(req));
		if (!qname.isSet()) {
			return redirectTo500(res);
		}
		Format format = getFormat(req);

		String data = req.getParameter("data");
		String predicateQname = req.getParameter("predicate_qname");
		String objectLiteral = req.getParameter("objectliteral");
		String objectResource = req.getParameter("objectresource");
		String langCode = req.getParameter("langcode");
		String contextQname = req.getParameter("context_qname");

		if (!given(data) && !given(predicateQname)) {
			data = readBody(req);
		}

		if (!given(data) && !given(predicateQname)) {
			throw new IllegalArgumentException("You must give 'data' or 'predicate_qname' parameter");
		}

		TriplestoreDAO dao = getTriplestoreDAO();
		if (given(data)) {
			put(qname, data, format, dao);
		} else {
			put(qname, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		}

		return processGet(req, res);
	}

	public static void put(Qname qname, String predicateQname, String objectResource, String objectLiteral, String langCode, String contextQname, TriplestoreDAO dao) throws Exception {
		Statement statement = null;
		Predicate predicate = new Predicate(predicateQname);
		Context context = contextQname == null ? null : new Context(contextQname);

		if (objectResource != null) {
			statement = new Statement(predicate, new ObjectResource(objectResource), context);
		} else if (objectLiteral != null) {
			statement = new Statement(predicate, new ObjectLiteral(objectLiteral, langCode), context);
		} else {
			throw new IllegalArgumentException("You must give objectliteral or objectresource parameter.");
		}

		dao.store(new Subject(qname), statement);
	}

	public static void put(Qname qname, String data, Format format, TriplestoreDAO dao) throws Exception {
		Model model = null;
		if (format == Format.RDFXMLABBREV || format == Format.RDFXML) {
			model = new Model(data);
		} else {
			throw new UnsupportedOperationException("Not yet implemented for format: " + format.toString());
		}
		if (!qname.toString().contains(":")) {
			dao.addResource(qname);
		}
		dao.store(model);
	}

}
