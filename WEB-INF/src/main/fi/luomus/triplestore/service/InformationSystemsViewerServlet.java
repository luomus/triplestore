package fi.luomus.triplestore.service;

import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.Model;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

		Map<String, TreeSet<Model>> systems = loadSystems(dao);

		int activeCount = 0;
		for (Collection<Model> list : systems.values()) {
			activeCount += list.size();
		}
		activeCount -= systems.get(Sixfold.ABANDONED.toString()).size();

		//		Model system = systems.get("").get(0);
		//		system.getStatements("").iterator().next().getObjectLiteral().get
		//		
		//dao.getProperties("").getAllProperties().iterator().next().getRange().getValueFor("")
		//		
		responseData.setData("properties", dao.getProperties("KE.informationSystem"));
		responseData.setData("systems", systems);
		responseData.setData("activeCount", activeCount);

		return responseData;
	}

	Map<String, TreeSet<Model>> loadSystems(TriplestoreDAO dao) {
		try {
			return tryToLoad(dao);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Comparator<Model> SYSTEM_COMPARATOR = new Comparator<Model>() {
		@Override
		public int compare(Model m1, Model m2) {
			String name1 = getName(m1);
			String name2 = getName(m2);
			return name1.compareTo(name2);
		}

		private String getName(Model m) {
			for (Statement s : m.getStatements("KE.name")) {
				return s.getObjectLiteral().getContent().toLowerCase();
			}
			return "Ö";
		}
	};

	private Map<String, TreeSet<Model>> tryToLoad(TriplestoreDAO dao) throws Exception {
		Map<String, TreeSet<Model>> systems = new HashMap<>();
		for (Model system : dao.getSearchDAO().search("rdf:type", "KE.informationSystem")) {
			String state = getState(system);
			String publicity = getPublicity(system);
			String slot = resolveSixfoldSlot(state, publicity).toString();
			if (!systems.containsKey(slot)) {
				systems.put(slot, new TreeSet<Model>(SYSTEM_COMPARATOR));
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