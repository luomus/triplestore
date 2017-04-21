//package fi.luomus.triplestore.taxonomy.iucn.runnable;
//import fi.luomus.commons.config.Config;
//import fi.luomus.commons.config.ConfigReader;
//import fi.luomus.commons.containers.rdf.Model;
//import fi.luomus.commons.containers.rdf.Qname;
//import fi.luomus.commons.containers.rdf.Statement;
//import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
//import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
//import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
//import fi.luomus.commons.utils.FileUtils;
//import fi.luomus.triplestore.dao.DataSourceDefinition;
//import fi.luomus.triplestore.dao.SearchParams;
//import fi.luomus.triplestore.dao.TriplestoreDAOConst;
//import fi.luomus.triplestore.dao.TriplestoreDAOImple;
//import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;
//import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
//import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse;
//
//import java.io.File;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//
//public class AlueellinenUhanalaisuus {
//
//	private static org.apache.tomcat.jdbc.pool.DataSource dataSource;
//	private static TriplestoreDAOImple triplestoreDAO;
//	private static ExtendedTaxonomyDAOImple taxonomyDAO;
//	
//	public static void main(String[] args) {
//		try {
//			Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
//			TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
//			dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
//			triplestoreDAO = new TriplestoreDAOImple(dataSource, new Qname("MA.5"));
//			taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO, new ErrorReporingToSystemErr());
//			doit();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (dataSource != null) dataSource.close();
//		} 
//		System.out.println("done");
//	}
//
//	private static void doit() throws Exception {
//		File f = new File("C:/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/2015/linnut_Alueellisesti uhanalaiset linnut 2015_julkaistu.txt");
//		for (String line : FileUtils.readLines(f)) {
//			if (line.startsWith("\t")) continue;
//			System.out.println(line);
//			String[] parts = line.split("\t");
//			String sciname = parts[0];
//			Qname taxonId = getId(sciname);
//			if (!taxonId.isSet()) throw new IllegalStateException("Not found: " + sciname);
//			Qname evaluationId = getEvaluation(taxonId);
//			if (!evaluationId.isSet()) throw new IllegalStateException("Eval not found: " + sciname);
//			Map<Qname, Occurrence> occurrences = getOccurrences(evaluationId);
//			int areaSeq = 690;
//			for (int i = 3; i < parts.length-1; i++) {
//				Qname areaId = new Qname("ML." + (areaSeq++));
//				String status = parts[i];
//				if (!occurrences.containsKey(areaId)) {
//					Occurrence occurrence = new Occurrence(null, areaId, null);
//					occurrences.put(areaId, occurrence);
//				}
//				Occurrence occurrence = occurrences.get(areaId);
//				System.out.println("area " + areaId + " : " + status + " \t old status : " + occurrence.getStatus() + " " + occurrence.getThreatened() + " " + occurrence.getYear());
//				Qname statusId = toStatus(status);
//				boolean threatened = status.equals("RT");
//				if (threatened || !statusId.equals(occurrence.getStatus())) {
//					boolean newOccurrence = !given(occurrence.getId());
//					occurrence.setStatus(statusId);
//					occurrence.setThreatened(threatened);
//					occurrence.setYear(2015);
//					triplestoreDAO.store(taxonId, occurrence);
//					System.out.println("Stored " + occurrence);
//					if (newOccurrence) {
//						System.out.println("exec addstatement('"+evaluationId+"', '"+IucnDAO.HAS_OCCURRENCE_PREDICATE+"', '"+occurrence.getId()+"');");
//					}
//				}
//			}
//		}
//		
//	}
//
//	private static boolean given(Qname id) {
//		return id != null && id.isSet();
//	}
//
//	private static Qname toStatus(String status) {
//		if (status.equals("-")) return new Qname("MX.doesNotOccur");
//		if (status.equals("X")) return new Qname("MX.typeOfOccurrenceOccurs");
//		if (status.equals("RT")) return new Qname("MX.typeOfOccurrenceOccurs");
//		if (status.equals("RE")) return new Qname("MX.typeOfOccurrenceExtirpated");
//		throw new UnsupportedOperationException(status);
//	}
//
//	private static Map<Qname, Occurrence> getOccurrences(Qname evaluationId) throws Exception {
//		Model model = triplestoreDAO.get(evaluationId);
//		Map<Qname, Occurrence> occurrences = new HashMap<>();
//		for (Statement s : model.getStatements("MKV.hasOccurrence")) {
//			Qname occurenceId = new Qname(s.getObjectResource().getQname());
//			Model occNode = triplestoreDAO.get(occurenceId);
//			Qname areaId = new Qname(occNode.getStatements("MO.area").get(0).getObjectResource().getQname());
//			Qname areaStatus = new Qname(occNode.getStatements("MO.status").get(0).getObjectResource().getQname());
//			Occurrence occurrence = new Occurrence(occurenceId, areaId, areaStatus);
//			if (occNode.hasStatements("MO.threatened")) {
//				if (occNode.getStatements("MO.threatened").get(0).getObjectLiteral().getContent().equals("true")) {
//					occurrence.setThreatened(true);
//				}
//			}
//			occurrence.setYear(Integer.valueOf(occNode.getStatements("MO.year").get(0).getObjectLiteral().getContent()));
//			if (occurrence.getYear() != 2015) throw new IllegalStateException("Invalud year " + evaluationId + " " + occurenceId + " " + occurrence.getYear());
//			occurrences.put(areaId, occurrence);
//		}
//		return occurrences;
//	}
//
//	private static Qname getEvaluation(Qname taxonId) throws Exception {
//		Collection<Model> models  = triplestoreDAO.getSearchDAO().search(new SearchParams(1000, 0).objectresource(taxonId.toString()).predicate("MKV.evaluatedTaxon"));
//		for (Model m : models) {
//			if (m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent().equals("2015")) {
//				return new Qname(m.getSubject().getQname());
//			}
//		}
//		return new Qname(null);
//	}
//
//	private static Qname getId(String sciname) throws Exception {
//		TaxonSearchResponse res = taxonomyDAO.searchInternal(new TaxonSearch(sciname).onlyExact());
//		if (res.getExactMatches().size() != 1) return new Qname(null);
//		return res.getExactMatches().get(0).getTaxon().getQname();
//	}
//
//}
