package fi.luomus.triplestore.taxonomy.iucn.runnable;
import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.jdbc.pool.DataSource;

public class JulkaisutSisaan {

	private static TriplestoreDAO triplestoreDAO;

	public static void main(String[] args) {
		DataSource dataSource = null;
		try {
			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));
			process();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		}
		System.out.println("done");
	}

	private static void process() throws Exception {
		File f = new File("C:/apache-tomcat/webapps/triplestore/src/runnables/fi/luomus/triplestore/taxonomy/iucn/runnable/iucn-julkaisut.txt");
		List<Publication> publications = getPublications(f);
		for (Publication p : publications) {
			//triplestoreDAO.storePublication(p);
		}
	}

	private static List<Publication> getPublications(File f) throws FileNotFoundException, IOException {
		List<Publication> publications = new ArrayList<>();
		StringBuilder b = new StringBuilder();
		List<String> lines = FileUtils.readLines(f);
		int i = 0;
		for (String line : lines) {
			if (line.equals("END")) return publications;
			String nextLine = lines.get(i+1);
			b.append(line);
			if (lastLineOfCitation(line, nextLine)) {
				ready(b);
				b = new StringBuilder();
			} else {
				b.append(" ");
			}
			i++;
		}
		return publications;
	}

	private static boolean lastLineOfCitation(String line, String nextLine) {
		return (line.endsWith(".|") || line.length() < 39) && !nextLine.startsWith("&") && !(nextLine.length() < 50) && !nextLine.startsWith("(") && !Character.isDigit(nextLine.charAt(0));
	}

	private static void ready(StringBuilder b) {
		String s = b.toString();
		s = s.replace("|", "");
		while (s.contains("  ")) {
			s = s.replace("  ", " ");
		}
		s = s.trim();
		if (s.length() > 0) {
			System.out.println(s);
			System.out.println();
		}
	}

}
