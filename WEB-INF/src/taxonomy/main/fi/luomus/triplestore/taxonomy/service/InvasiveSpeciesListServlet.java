package fi.luomus.triplestore.taxonomy.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;

@WebServlet(urlPatterns = {"/taxonomy-editor/invasive/*"})
public class InvasiveSpeciesListServlet extends TaxonomyEditorBaseServlet {

	private static final Qname LIST_RESOURCE = new Qname("HBE.historicInvasiveList");
	private static final long serialVersionUID = 456215071020277850L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		ResponseData responseData = initResponseData(req);

		Set<Qname> currentlyDefinedInvasiveSpecies = getCurrentlyDefinedInvasiveSpecies(); 		
		Set<Qname> historicInvasiveSpecies = getHistoricInvasiveSpecies();
		Set<Qname> union = getUnion(currentlyDefinedInvasiveSpecies, historicInvasiveSpecies);
		Set<Qname> notInHistoric = getDifference(currentlyDefinedInvasiveSpecies, historicInvasiveSpecies);
		if (!notInHistoric.isEmpty()) {
			storeHistoric(union);
		}

		TaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		List<Taxon> invasiveSpecies = new ArrayList<>();
		for (Qname id : union) {
			if (taxonomyDAO.getTaxonContainer().hasTaxon(id)) {
				invasiveSpecies.add(taxonomyDAO.getTaxon(id));
			} else {
				Taxon missing = new Taxon(id, null);
				missing.setScientificName("MISSING!");
				invasiveSpecies.add(missing);
			}
		}
		Collections.sort(invasiveSpecies, Taxon.TAXON_ALPHA_COMPARATOR);

		return responseData
				.setViewName("invasiveSpeciesList")
				.setData("species", invasiveSpecies)
				.setData("adminStatuses", getTriplestoreDAO().getProperty(new Predicate("MX.hasAdminStatus")).getRange())
				.setData("groups", getTriplestoreDAO().getProperty(new Predicate("HBE.invasiveSpeciesMainGroup")).getRange())
				.setData("establishments", getTriplestoreDAO().getProperty(new Predicate("MX.invasiveSpeciesEstablishment")).getRange());
	}

	private void storeHistoric(Set<Qname> ids) throws Exception {
		StringBuilder b = new StringBuilder();
		Iterator<Qname> i = ids.iterator();
		while (i.hasNext()) {
			b.append(i.next());
			if (i.hasNext()) b.append(",");
		}
		Model model = new Model(LIST_RESOURCE);
		model.addStatement(new Statement(new Predicate(LIST_RESOURCE), new ObjectLiteral(b.toString())));
		getTriplestoreDAO().store(model);
	}

	private Set<Qname> getUnion(Set<Qname> currentlyDefinedInvasiveSpecies, Set<Qname> historicInvasiveSpecies) {
		Set<Qname> union = new HashSet<>();
		union.addAll(currentlyDefinedInvasiveSpecies);
		union.addAll(historicInvasiveSpecies);
		return union;
	}

	private Set<Qname> getDifference(Set<Qname> currentlyDefinedInvasiveSpecies, Set<Qname> historicInvasiveSpecies) {
		Set<Qname> difference = new HashSet<>(currentlyDefinedInvasiveSpecies);
		difference.removeAll(historicInvasiveSpecies);
		return difference;
	}

	private Set<Qname> getHistoricInvasiveSpecies() throws Exception {
		Model m = getTriplestoreDAO().get(LIST_RESOURCE);
		if (m.isEmpty()) return Collections.emptySet();
		if (!m.hasStatements(LIST_RESOURCE)) return Collections.emptySet();
		Statement s = m.getStatements(LIST_RESOURCE).get(0);
		if (!s.isLiteralStatement()) return Collections.emptySet();
		String list = s.getObjectLiteral().getContent();
		Set<Qname> ids = new HashSet<>();
		for (String id : list.split(Pattern.quote(","))) {
			ids.add(new Qname(id));
		}
		return ids;
	}

	private Set<Qname> getCurrentlyDefinedInvasiveSpecies() throws Exception {
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			TriplestoreDAOImple dao = (TriplestoreDAOImple) getTriplestoreDAO();
			con = getTriplestoreDAO().openConnection();
			String sql = "" +
			" SELECT DISTINCT subjectname FROM "+dao.getSchema()+".rdf_statementview " + 
			" WHERE subjectname IN (  " +
			"   SELECT subjectname FROM "+dao.getSchema()+".rdf_statementview where predicatename = 'MX.invasiveSpeciesEstablishment' UNION  " +
			"   SELECT subjectname FROM "+dao.getSchema()+".rdf_statementview where predicatename = 'MX.invasiveSpeciesCategory' UNION " +
			"   SELECT subjectname FROM "+dao.getSchema()+".rdf_statementview where predicatename = 'HBE.invasiveSpeciesMainGroup' UNION " +
			"   SELECT subjectname FROM "+dao.getSchema()+".rdf_statementview where predicatename = 'MX.hasAdminStatus' AND objectname IN ( " +
			"       'MX.nationallySignificantInvasiveSpecies',  " +
			"       'MX.euInvasiveSpeciesList',  " +
			"       'MX.quarantinePlantPest', " +
			"       'MX.nationalInvasiveSpeciesStrategy', " +
			"       'MX.otherInvasiveSpeciesList' " +
			"     )" +
			" ) " +
			" AND predicatename = 'MX.nameAccordingTo' " +
			" AND (objectname = 'MR.1' OR objectname is null) ";
			p = con.prepareStatement(sql);
			rs = p.executeQuery();
			Set<Qname> ids = new HashSet<>();
			while (rs.next()) {
				ids.add(new Qname(rs.getString(1)));
			}
			return ids;
		} finally {
			Utils.close(p, rs, con);
		}
	}

}
