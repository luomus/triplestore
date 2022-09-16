//package fi.luomus.triplestore.taxonomy.iucn.runnable;
//import fi.luomus.commons.config.Config;
//import fi.luomus.commons.config.ConfigReader;
//import fi.luomus.commons.containers.Publication;
//import fi.luomus.commons.containers.rdf.Qname;
//import fi.luomus.commons.reporting.ErrorReportingToSystemErr;
//import fi.luomus.commons.utils.FileUtils;
//import fi.luomus.commons.utils.Utils;
//import fi.luomus.triplestore.dao.DataSourceDefinition;
//import fi.luomus.triplestore.dao.TriplestoreDAO;
//import fi.luomus.triplestore.dao.TriplestoreDAOConst;
//import fi.luomus.triplestore.dao.TriplestoreDAOImple;
//import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
//import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.regex.Pattern;
//
//import org.apache.tomcat.jdbc.pool.DataSource;
//
//public class JulkaisutSisaan {
//
//	private static TriplestoreDAO triplestoreDAO;
//	private static ExtendedTaxonomyDAO taxonomyDAO;
//	
//	public static void main(String[] args) {
//		DataSource dataSource = null;
//		try {
//			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
//			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
//			//dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
//			dataSource = null;
//			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));
//			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO, new ErrorReportingToSystemErr());
//			process();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (dataSource != null) dataSource.close();
//		}
//		System.out.println("done");
//	}
//
//	private static void process() throws Exception {
//		File f = new File("C:/apache-tomcat/webapps/triplestore/src/runnables/fi/luomus/triplestore/taxonomy/iucn/runnable/iucn-julkaisut-kasitelty.txt");
//		Collection<Publication> newPublications = getPublications(f);
//		Collection<Publication> existingPublications = taxonomyDAO.getPublications().values();
//		for (Publication p : newPublications) {
//			Collection<Publication> similiar = getSimiliar(p, existingPublications);
//			if (similiar.isEmpty()) {
//				Qname qname = triplestoreDAO.getSeqNextValAndAddResource("MP");
//				Publication p2 = new Publication(qname);
//				p2.setCitation(p.getCitation());
//				triplestoreDAO.storePublication(p2);
//				Utils.debug(qname, p2.getCitation());
//			}
//		}
//	}
//
//	private static Collection<Publication> getSimiliar(Publication p, Collection<Publication> existingPublications) {
//		List<Publication> similiar = new ArrayList<>();
//		for (Publication e : existingPublications) {
//			String pFirst = getFirstWord(p.getCitation());
//			String eFirst = getFirstWord(e.getCitation());
//			if (!pFirst.equalsIgnoreCase(eFirst)) continue;
//			
//			if (p.getCitation().contains(":") && !e.getCitation().contains(":")) continue;
//			if (!p.getCitation().contains(":") && e.getCitation().contains(":")) continue;
//			if (p.getCitation().contains(":") && e.getCitation().contains(":")) {
//				String pTitle = getFirstWord(p.getCitation().split(Pattern.quote(":"))[1].trim());
//				String eTitle = getFirstWord(e.getCitation().split(Pattern.quote(":"))[1].trim());
//				if (!pTitle.equalsIgnoreCase(eTitle)) continue; 
//			}
//				
//			similiar.add(e);
//		}
//		return similiar;
//	}
//
//	private static String getFirstWord(String citation) {
//		String s = citation.split(" ")[0].trim();
//		if (!s.equalsIgnoreCase("the") && !s.equalsIgnoreCase("suomen")) return s;
//		return citation.split(" ")[1].trim();
//	}
//
//	private static List<Publication> getPublications(File f) throws FileNotFoundException, IOException {
//		List<Publication> publications = new ArrayList<>();
//		List<String> lines = FileUtils.readLines(f);
//		for (String line : lines) {
//			line = line.trim();
//			if (line.isEmpty()) continue;
//			if (line.contains("http:")) continue;
//			while (line.contains("  ")) {
//				line = line.replace("  ", " ");
//			}
//			line = line.trim();
//			publications.add(new Publication(null).setCitation(line));
//		}
//		return publications;
//	}
//
//}
