package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonSearchResponse.Match;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;

public class Backcasting2000Sisaan {

	private static org.apache.tomcat.jdbc.pool.DataSource dataSource;
	private static TriplestoreDAOImple triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;

	private static final Map<String, Set<Qname>> INFORMAL_GROUP;
	static {
		INFORMAL_GROUP = new HashMap<String, Set<Qname>>();
		INFORMAL_GROUP.put("Helttasienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		INFORMAL_GROUP.put("Hämähäkit_siirto.csv", Utils.set(new Qname("MVL.38")));
		INFORMAL_GROUP.put("Lichens", Utils.set(new Qname("MVL.25")));
		INFORMAL_GROUP.put("Jäytiäiset_siirto.csv", Utils.set(new Qname("MVL.227")));
		INFORMAL_GROUP.put("Kalat_siirto.csv", Utils.set(new Qname("MVL.27")));
		INFORMAL_GROUP.put("Kierresiipiset_siirto.csv", Utils.set(new Qname("MVL.229")));
		INFORMAL_GROUP.put("Kolmisukahäntäiset_siirto.csv", Utils.set(new Qname("MVL.301")));
		INFORMAL_GROUP.put("Dragonflies", Utils.set(new Qname("MVL.36"), new Qname("MVL.222")));
		INFORMAL_GROUP.put("Kotelosienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		INFORMAL_GROUP.put("Beetles", Utils.set(new Qname("MVL.33")));
		INFORMAL_GROUP.put("Kupusienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		INFORMAL_GROUP.put("Kärpäset_siirto.csv", Utils.set(new Qname("MVL.224")));
		INFORMAL_GROUP.put("Polypores", Utils.set(new Qname("MVL.233")));
		INFORMAL_GROUP.put("Levät_siirto.csv", Utils.set(new Qname("MVL.22")));
		INFORMAL_GROUP.put("Limasienet_siirto.csv", Utils.set(new Qname("MVL.321")));
		INFORMAL_GROUP.put("Birds", Utils.set(new Qname("MVL.1")));
		INFORMAL_GROUP.put("Maasiirat_lukit_valeskorpionit_siirto.csv", Utils.set(new Qname("MVL.215"), new Qname("MVL.235"), new Qname("MVL.236")));
		INFORMAL_GROUP.put("Herptiles", Utils.set(new Qname("MVL.26")));
		INFORMAL_GROUP.put("Nilviäiset_siirto.csv", Utils.set(new Qname("MVL.239"), new Qname("MVL.240")));
		INFORMAL_GROUP.put("True bugs", Utils.set(new Qname("MVL.34")));
		INFORMAL_GROUP.put("Nivelmadot_siirto.csv", Utils.set(new Qname("MVL.241")));
		INFORMAL_GROUP.put("Butterflies", Utils.set(new Qname("MVL.31")));
		INFORMAL_GROUP.put("Piensienet_siirto.csv", Utils.set(new Qname("MVL.233")));
		INFORMAL_GROUP.put("Pistiäiset_siirto.csv", Utils.set(new Qname("MVL.30")));
		INFORMAL_GROUP.put("Punkit_siirto.csv", Utils.set(new Qname("MVL.234")));
		INFORMAL_GROUP.put("Vascular Plants", Utils.set(new Qname("MVL.343")));
		INFORMAL_GROUP.put("Ripsiäiset_siirto.csv", Utils.set(new Qname("MVL.228")));
		INFORMAL_GROUP.put("Bryophytes", Utils.set(new Qname("MVL.23")));
		INFORMAL_GROUP.put("Suorasiipiset_siirto.csv", Utils.set(new Qname("MVL.223"), new Qname("MVL.230"), new Qname("MVL.225")));
		INFORMAL_GROUP.put("Sääsket_siirto.csv", Utils.set(new Qname("MVL.224")));
		INFORMAL_GROUP.put("Tuhatjalkaiset_siirto.csv", Utils.set(new Qname("MVL.37")));
		INFORMAL_GROUP.put("Verkkosiipiset_siirto.csv", Utils.set(new Qname("MVL.226")));
		INFORMAL_GROUP.put("Vesiperhoset_siirto.csv", Utils.set(new Qname("MVL.222")));
	}

	public static void main(String[] args) {
		try {
			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));
			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO, new ErrorReporingToSystemErr());
			doit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dataSource != null) dataSource.close();
		} 
		System.out.println("done");
	}

	private static void doit() throws Exception {
		File f = new File("C:/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/RLI/2000_AllData.csv");
		int i = 0;
		for (String line : FileUtils.readLines(f)) {
			if (i++ % 1000 == 0) System.out.println(i);
			if (line.startsWith("Group")) continue;
			//System.out.println(line);
			String[] parts = line.split("\t");
			String group = parts[0];
			String sciname = parts[1];
			if (sciname.contains("(")) {
				sciname = sciname.substring(0, sciname.indexOf('('));
			}
			Qname backCasted2000 = new Qname("MX.iucn"+parts[2]);
			Qname status2010 = new Qname("MX.iucn"+parts[3]);

			Qname taxonId = getTaxonId(group, sciname);
			if (taxonId == null) {
				if (sciname.contains("ssp.")) {
					taxonId = getTaxonId(group, sciname.replace("ssp.", "subsp."));
				} else if (sciname.contains("subsp.")) {
					taxonId = getTaxonId(group, sciname.replace("subsp.", "ssp."));
				}
			}
			if (taxonId == null && group.equals("Butterflies")) {
				taxonId = getWithFinnishName(sciname, "C:/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/2010/Perhoset_siirto.csv", group);
			}
			if (taxonId == null) {
				System.out.println("Not found " + sciname + "  | " + group);
			}
			//Utils.debug(sciname, taxonId);
		}

	}

	private static Qname getWithFinnishName(String sciname, String filename, String group) throws Exception {
		for (String line : FileUtils.readLines(new File(filename))) {
			String[] parts = line.split(Pattern.quote("|"));
			String lineSciname = parts[1].trim();
			if (sciname.trim().equalsIgnoreCase(lineSciname)) {
				String fiName = parts[10];
				return getTaxonId(group, fiName);
			}
		}
		return null;
	}

	private static Qname getTaxonId(String group, String sciname) throws Exception {
		Qname taxonId = IUCN2010Sisaan.getFixedQnameForName(sciname);
		if (taxonId != null) return taxonId;

		Collection<Taxon> taxons = getTaxons(sciname);
		if (taxons.isEmpty()) return null;
		
		Taxon taxon = getAcceptedTaxon(taxons, group);
		if (taxon == null) {
			return null;
		}
		return taxon.getQname();
	}

	private static Taxon getAcceptedTaxon(Collection<Taxon> taxons, String group) {
		Set<Qname> acceptedGroups = INFORMAL_GROUP.get(group);  
		for (Taxon t : taxons) {
			Set<Qname> taxonsGroups = t.getInformalTaxonGroups();
			for (Qname g : taxonsGroups) {
				if (acceptedGroups.contains(g)) {
					return t;
				}
			}
		}
		return null;
	}

	private static boolean given(Qname id) {
		return id != null && id.isSet();
	}

	private static Qname toStatus(String status) {
		if (status.equals("-")) return new Qname("MX.doesNotOccur");
		if (status.equals("X")) return new Qname("MX.typeOfOccurrenceOccurs");
		if (status.equals("RT")) return new Qname("MX.typeOfOccurrenceOccurs");
		if (status.equals("RE")) return new Qname("MX.typeOfOccurrenceExtirpated");
		throw new UnsupportedOperationException(status);
	}

	private static Map<Qname, Occurrence> getOccurrences(Qname evaluationId) throws Exception {
		Model model = triplestoreDAO.get(evaluationId);
		Map<Qname, Occurrence> occurrences = new HashMap<>();
		for (Statement s : model.getStatements("MKV.hasOccurrence")) {
			Qname occurenceId = new Qname(s.getObjectResource().getQname());
			Model occNode = triplestoreDAO.get(occurenceId);
			Qname areaId = new Qname(occNode.getStatements("MO.area").get(0).getObjectResource().getQname());
			Qname areaStatus = new Qname(occNode.getStatements("MO.status").get(0).getObjectResource().getQname());
			Occurrence occurrence = new Occurrence(occurenceId, areaId, areaStatus);
			if (occNode.hasStatements("MO.threatened")) {
				if (occNode.getStatements("MO.threatened").get(0).getObjectLiteral().getContent().equals("true")) {
					occurrence.setThreatened(true);
				}
			}
			occurrence.setYear(Integer.valueOf(occNode.getStatements("MO.year").get(0).getObjectLiteral().getContent()));
			if (occurrence.getYear() != 2015) throw new IllegalStateException("Invalud year " + evaluationId + " " + occurenceId + " " + occurrence.getYear());
			occurrences.put(areaId, occurrence);
		}
		return occurrences;
	}

	private static Qname getEvaluation(Qname taxonId) throws Exception {
		Collection<Model> models  = triplestoreDAO.getSearchDAO().search(new SearchParams(1000, 0).objectresource(taxonId.toString()).predicate("MKV.evaluatedTaxon"));
		for (Model m : models) {
			if (m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent().equals("2015")) {
				return new Qname(m.getSubject().getQname());
			}
		}
		return new Qname(null);
	}

	private static Collection<Taxon> getTaxons(String sciname) throws Exception {
		TaxonSearchResponse res = taxonomyDAO.search(new TaxonSearch(sciname).onlyExact());
		List<Taxon> t = new ArrayList<>();
		for (Match m : res.getExactMatches()) {
			t.add(m.getTaxon());
		}
		return t; 
	}

}
