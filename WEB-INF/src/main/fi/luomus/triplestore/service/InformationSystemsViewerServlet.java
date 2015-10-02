package fi.luomus.triplestore.service;

import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected.CacheLoader;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/it/*"})
public class InformationSystemsViewerServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 6850858604573984943L;

	public static enum Sixfold { PUBLIC_DEVELOPMENT, PUBLIC_PRODUCTION, INTERNAL_DEVELOPMENT, INTERNAL_PRODUCTION, ADMIN, ABANDONED, UKNOWN };

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return getUser(req).isAdmin();
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.initResponseData(req).setViewName("it-sixfold");

		if ("dependency-graph".equals(req.getParameter("view"))) responseData.setViewName("it-dependency-graph");

		TriplestoreDAO dao = getTriplestoreDAO();
		responseData.setData("properties", dao.getProperties("KE.informationSystem"));
		responseData.setData("systemFourfoldMap", SYSTEMS_CACHE.get(dao));

		return responseData;
	}

	private static final SingleObjectCacheResourceInjected<Map<Sixfold, List<Model>>, TriplestoreDAO> SYSTEMS_CACHE = 
			new SingleObjectCacheResourceInjected<Map<Sixfold, List<Model>>, TriplestoreDAO>(new SystemsLoader(), 60*3); 

	private static class SystemsLoader implements CacheLoader<Map<Sixfold, List<Model>>, TriplestoreDAO> {

		public Map<Sixfold, List<Model>> load(TriplestoreDAO dao) {
			try {
				return tryToLoad(dao);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Map<Sixfold, List<Model>> tryToLoad(TriplestoreDAO dao) throws Exception {
			Map<Sixfold, List<Model>> systems = new HashMap<>();
			for (Model system : dao.getSearchDAO().search("rdf:type", "KE.informationSystem")) {
				String state = getState(system);
				String publicity = getPublicity(system);
				Sixfold slot = resolveSixfoldSlot(state, publicity);
				if (!systems.containsKey(slot)) {
					systems.put(slot, new ArrayList<Model>());
				}
				systems.get(slot).add(system);
			}
			return systems;
		}

		private String getPublicity(Model system) {
			List<Statement> publicities = system.getStatements("KE.publicity");
			if (publicities.isEmpty()) return "";
			if (publicities.size() > 1) throw new IllegalStateException("Too many publicities for " + system.getSubject().getQname());
			return publicities.get(0).getObjectResource().getQname();
		}

		private String getState(Model system) {
			List<Statement> states = system.getStatements("KE.state");
			if (states.isEmpty()) return "";
			if (states.size() > 1) throw new IllegalStateException("Too many states for " + system.getSubject().getQname());
			return states.get(0).getObjectResource().getQname();
		}

		private Sixfold resolveSixfoldSlot(String state, String publicity) {
			if (state.equals("KE.abandonedState")) 
				return Sixfold.ABANDONED;
			if (publicity.equals("KE.adminOnly")) 
				return Sixfold.ADMIN;
			if (state.equals("KE.productionState") && publicity.equals("KE.inPublicUse")) {
				return Sixfold.PUBLIC_PRODUCTION;
			}
			if (state.equals("KE.productionState") && publicity.equals("KE.inUseByLuomus")) {
				return Sixfold.INTERNAL_PRODUCTION;
			}
			if (state.equals("KE.developmentState") && publicity.equals("KE.inPublicUse")) {
				return Sixfold.PUBLIC_DEVELOPMENT;
			}
			if (state.equals("KE.developmentState") && publicity.equals("KE.inUseByLuomus")) {
				return Sixfold.INTERNAL_DEVELOPMENT;
			}
			return Sixfold.UKNOWN;
		}
	}
}