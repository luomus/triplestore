package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.triplestore.dao.NamespacesDAO;
import fi.luomus.triplestore.dao.NamespacesDAOImple;
import fi.luomus.triplestore.models.Namespace;

@WebServlet(urlPatterns = {"/api/namespaces/*"})
public class PulicNamespacesServlet extends ApiServlet {

	private static final long serialVersionUID = -5846147682122073946L;

	private CacheLoader<ResponseData> loader = new CacheLoader<ResponseData>() {

		@Override
		public ResponseData load() {
			try {
				return generateResponse();
			} catch (Exception e) {
				getErrorReporter().report("Loading namespaces", e);
				throw new RuntimeException(e);
			}
		}
	};

	private final SingleObjectCache<ResponseData> cache = new SingleObjectCache<>(loader, 15, TimeUnit.MINUTES);

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return cache.get();
	}

	protected ResponseData generateResponse() throws Exception, IOException {
		JSONArray response = new JSONArray();
		for (Namespace ns : getDao().getNamespaces()) {
			response.appendObject(generateResponse(ns));
		}
		return jsonResponse(response);
	}

	private JSONObject generateResponse(Namespace ns) {
		JSONObject o = new JSONObject();
		o.setString("namespace_id", ns.getNamespace());
		o.setString("person_in_charge", ns.getPersonInCharge());
		o.setString("purpose", ns.getPurpose());
		o.setString("namespace_type", ns.getType());
		o.setString("qname_prefix", ns.getQnamePrefix());
		return o;
	}

	private NamespacesDAO getDao() {
		return new NamespacesDAOImple(getConfig());
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}

}
