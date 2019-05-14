package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileUtils;

public class IUCN_LC_generointi {

	public static void main(String[] args) throws Exception {
		HttpClientService client = new HttpClientService("https://triplestore.luomus.fi", "user", "pass");
		try {
			for (Qname taxonId : taxonIds()) {
				Qname evaluationid = getEvaluationId(client);
				Qname habitatId = getEvaluationId(client);
				Model model = new Model(evaluationid);
				model.setType("MKV.iucnRedListEvaluation");
				model.addStatementIfObjectGiven("MKV.state", new Qname("MKV.stateReady"));
				model.addStatementIfObjectGiven("MKV.redListStatus", new Qname("MX.iucnLC"));
				model.addStatementIfObjectGiven("MKV.evaluatedTaxon", taxonId);
				model.addStatementIfObjectGiven("MKV.lastModified", DateUtils.getCurrentDate());
				model.addStatementIfObjectGiven("MKV.evaluationYear", "2019");
				model.addStatementIfObjectGiven("MKV.primaryHabitat", habitatId);
				model.addStatementIfObjectGiven("MKV.editNotes", "Merkitty LC-luokkaan massa-ajolla.; 18.4.2018");
				Model habitat = new Model(habitatId);
				habitat.setType("MKV.habitatObject");
				habitat.addStatementIfObjectGiven("MKV.habitat", new Qname("MKV.habitatM"));
				habitat.addStatementIfObjectGiven("sortOrder", "0");
				System.out.println(model.getRDF());
				System.out.println(habitat.getRDF());
				store(habitat, client);
				store(model, client);
			}
		}
		finally {
			client.close();
		}
		System.out.println("done");
	}

	private static void store(Model model, HttpClientService client) throws Exception {
		HttpPost post = new HttpPost("https://triplestore.luomus.fi/"+model.getSubject().toString());
		HttpEntity entity = new ByteArrayEntity(model.getRDF().getBytes("UTF-8"), ContentType.APPLICATION_XML);
		post.setEntity(entity);
		CloseableHttpResponse res = null;
		try {
			res = client.execute(post);
			if (res.getStatusLine().getStatusCode() != 200) throw new Exception(""+ res.getStatusLine().getStatusCode());
		} finally {
			if (res != null) res.close();
		}
	}

	private static Qname getEvaluationId(HttpClientService client) throws ClientProtocolException, IOException {
		return new Qname(client.contentAsJson(new HttpGet("https://triplestore.luomus.fi/uri/MKV")).getObject("response").getString("qname"));
	}

	private static Set<Qname> taxonIds() throws FileNotFoundException, IOException {
		Set<Qname> taxonIds = new HashSet<>();
		File file = new File("c:/temp/lc-ksi.txt");
		for (String line : FileUtils.readLines(file)) {
			if (line.trim().isEmpty()) continue;
			taxonIds.add(new Qname(line));
		}
		return taxonIds;
	}

}
