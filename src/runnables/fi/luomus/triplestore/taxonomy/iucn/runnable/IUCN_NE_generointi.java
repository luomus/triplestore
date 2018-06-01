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

public class IUCN_NE_generointi {

	public static void main(String[] args) throws Exception {
		HttpClientService client = new HttpClientService("https://triplestore.luomus.fi", "esko-dev", "y4JsjjskSK3j3k3j");
		try {
			Set<Qname> taxonIds = taxonIds();
			for (Qname taxonId : taxonIds) {
				Qname evaluationid = getEvaluationId(client);
				Model model = new Model(evaluationid);
				model.setType("MKV.iucnRedListEvaluation");
				model.addStatementIfObjectGiven("MKV.state", new Qname("MKV.stateReady"));
				model.addStatementIfObjectGiven("MKV.redListStatus", new Qname("MX.iucnNE"));
				model.addStatementIfObjectGiven("MKV.evaluatedTaxon", taxonId);
				model.addStatementIfObjectGiven("MKV.lastModified", DateUtils.getCurrentDate());
				model.addStatementIfObjectGiven("MKV.evaluationYear", "2019");
				model.addStatementIfObjectGiven("MKV.editNotes", "Merkitty NE-luokkaan massa-ajolla.; 1.6.2018");
				System.out.println(model.getRDF());
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
			res.close();
		}
	}

	private static Qname getEvaluationId(HttpClientService client) throws ClientProtocolException, IOException {
		return new Qname(client.contentAsJson(new HttpGet("https://triplestore.luomus.fi/uri/MKV")).getObject("response").getString("qname"));
	}

	private static Set<Qname> taxonIds() throws FileNotFoundException, IOException {
		Set<Qname> taxonIds = new HashSet<>();
		for (String line : FileUtils.readLines(new File("c:/temp/ne-lajit.txt"))) {
			Qname q = new Qname(line.trim());
			if (q.isSet()) {
				taxonIds.add(q);
			}
		}
		return taxonIds;
	}

}
