package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonSearchResponse.Match;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.runnable.IUCNLineData.Mode;

public class IUCNGlobaalitSisaan {

	private static final Qname MISAPPLIED = new Qname("MX.hasMisappliedName");
	private static final String FILE_PATH = "C:/esko-local/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/globaalit";
	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;

	public static void main(String[] args) {
		DataSource dataSource = null;
		try {
			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));

			// prod mode XXX MUST USE PROD MODE WHEN LOADING DATA (dev is for test dry runs)
			//taxonomyDAO = new ExtendedTaxonomyDAOImple(config, false, triplestoreDAO, new ErrorReporingToSystemErr()); 

			// dev mode
			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, true, triplestoreDAO, new ErrorReporingToSystemErr());

			process();
			taxonomyDAO.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		}
		System.out.println("done");
	}

	private static void process() throws Exception {
		File folder = new File(FILE_PATH);
		for (File f : folder.listFiles()) {
			if (!f.isFile()) continue;
			if (!f.getName().endsWith(".csv")) continue;
			System.out.println(f.getName());
			process(f);
		}
	}

	private static void process(File f) throws Exception {
		List<String> lines = FileUtils.readLines(f);
		int i = 0;
		for (String line : lines) {
			if (line.startsWith("Species ID")) continue;
			i++;
			line = line.trim();
			if (line.isEmpty()) continue;
			process(line, f, i, lines.size());
		}
	}

	private static void process(String line, File f, int i, int total) throws Exception {
		String[] parts = line.split(Pattern.quote("|"));
		IUCNLineData data = new IUCNLineData(Mode.GLOBAL, parts);
		process(data, f, i, total);
	}

	private static void process(IUCNLineData data, File f, int i, int total) {
		try {
			System.out.println(i + " / " + total + "\t" + data.getScientificName() + " " + data.getTaxonQname());
			TaxonSearchResponse response = new TaxonSearchResponse(new TaxonSearch("foo"));
			if (given(data.getTaxonQname())) {
				response = taxonomyDAO.search(new TaxonSearch(data.getTaxonQname()).onlyExact());
			}
			if (response.getExactMatches().isEmpty()) {
				response = searchByScientificName(data.getScientificName(), response);
			}
			if (response.getExactMatches().isEmpty()) {
				for (String synonym : data.getSynonyms()) {
					response = searchByScientificName(synonym, response);
				}
			}
			if (response.getExactMatches().isEmpty()) {
				reportTaxonNotFound("Tuntematon nimi", data, f);
				return;
			}

			Set<Qname> matchingQnames = matchTaxonQnames(response.getExactMatches());
			if (matchingQnames.size() > 1) {
				reportTaxonNotFound("Löytyi " + matchingQnames.size() + " osumaa: " + matchingQnames, data, f);
				return;
			}
			process(data, response.getExactMatches().get(0), f);
		} catch (Exception e) {
			reportError(e, data, f);
		}	
	}

	private static TaxonSearchResponse searchByScientificName(String scientificName, TaxonSearchResponse response) throws Exception {
		if (!given(scientificName)) return response;
		response = taxonomyDAO.search(new TaxonSearch(scientificName).onlyExact());
		if (response.getExactMatches().size() > 1) {
			response = taxonomyDAO.search(new TaxonSearch(scientificName).onlyExact().addExlucedNameType(MISAPPLIED));
		}
		if (response.getExactMatches().isEmpty()) {
			String cleanedSciName = cleanScientificName(scientificName);
			if (given(cleanedSciName)) {
				response = taxonomyDAO.search(new TaxonSearch(cleanedSciName).onlyExact().addExlucedNameType(MISAPPLIED));
			}
		}
		return response;
	}

	private static boolean given(String s) {
		return s != null && s.length() > 0;
	}

	public static String cleanScientificName(String scientificName) {
		if (scientificName.contains(",")) {
			return scientificName.split(Pattern.quote(","))[0].trim();
		}
		if (!scientificName.contains("(")) return null;
		StringBuilder b = new StringBuilder();
		boolean inparenthesis = false;
		for (char c : scientificName.toCharArray()) {
			if (c == '(') {
				inparenthesis = true;
				continue;
			}
			if (c == ')') {
				inparenthesis = false;
				continue;
			}
			if (!inparenthesis) {
				b.append(c);
			}
		}
		String s = b.toString();
		while (s.contains("  ")) {
			s = s.replace("  ", " ");
		}
		return s.trim();
	}

	private static void process(IUCNLineData data, Match match, File f) throws Exception {
		System.out.println("Täsmäsi " + data.scientificName + " -> " + match.getTaxon().getQname());
		File file = new File("c:/temp/iucn/global_" + f.getName().replace(".csv", "_" + DateUtils.getCurrentDate() +".txt"));
		try {
			StringBuilder b = new StringBuilder();
			b.append(data.legacyInformalGroup).append("|");
			b.append(data.scientificName).append("|");
			b.append(data.synonyms.toString()).append("|");
			b.append("->").append("|");
			b.append(match.getMatchingName()).append("|");
			b.append(match.getNameType()).append("|");
			b.append("->").append("|");
			b.append(match.getTaxon().getQname()).append("|");
			b.append(match.getTaxon().getScientificName()).append("|");
			b.append(debug(match.getInformalGroups())).append("|");
			b.append(match.getTaxon().isFinnish() ? "Suomalainen" : "Ei");
			b.append("->").append("|");
			b.append(data.redListStatus).append("|");
			b.append(data.criteriaForStatus);
			FileUtils.writeToFile(file, b.toString()+"\n", "ISO-8859-1", true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// TODO save something
	}

	private static String debug(List<InformalTaxonGroup> informalGroups) {
		StringBuilder b = new StringBuilder();
		for (InformalTaxonGroup i : informalGroups) {
			b.append(i.getName().forLocale("fi")).append(" ");
		}
		return b.toString().trim();
	}

	private static Set<Qname> matchTaxonQnames(List<Match> exactMatches) {
		Set<Qname> set = new HashSet<>();
		for (Match m : exactMatches) {
			set.add(m.getTaxon().getQname());
		}
		return set;
	}

	private static void reportTaxonNotFound(String message, IUCNLineData data, File f) {
		File file = new File("c:/temp/iucn/missing_" + f.getName().replace(".csv", "_" + DateUtils.getCurrentDate() +".txt"));
		try {
			StringBuilder b = new StringBuilder();
			String id = given(data.taxonQname) ? data.taxonQname : data.getScientificName();

			b.append(data.legacyInformalGroup).append("|");
			b.append(id).append("|");
			b.append(message);
			FileUtils.writeToFile(file, b.toString()+"\n", "ISO-8859-1", true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static void reportError(Exception e, IUCNLineData data, File f) {
		File errorFile = new File("c:/temp/iucn/error_" + f.getName().replace(".csv", ".txt"));
		try {
			FileUtils.writeToFile(errorFile, data.getScientificName() + ":\n" + LogUtils.buildStackTrace(e, 5)+"\n", true);
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
