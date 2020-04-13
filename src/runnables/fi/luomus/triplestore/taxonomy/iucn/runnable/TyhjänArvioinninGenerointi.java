package fi.luomus.triplestore.taxonomy.iucn.runnable;

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

public class TyhjänArvioinninGenerointi {

	public static void main(String[] args) throws Exception {
		HttpClientService client = new HttpClientService("https://triplestore.luomus.fi", "user", "pass"); // XXX
		try {
			Set<Qname> taxonIds = taxonIds();
			for (Qname taxonId : taxonIds) {
				Qname evaluationid = getEvaluationId(client);
				Model model = new Model(evaluationid);
				model.setType("MKV.iucnRedListEvaluation");
				model.addStatementIfObjectGiven("MKV.state", new Qname("MKV.stateStarted"));
				model.addStatementIfObjectGiven("MKV.evaluatedTaxon", taxonId);
				model.addStatementIfObjectGiven("MKV.lastModified", DateUtils.getCurrentDate());
				model.addStatementIfObjectGiven("MKV.evaluationYear", "2010"); // XXX
				model.addStatementIfObjectGiven("MKV.editNotes", "Luotu tilapäinen arviointi joka täydennetään; 25.4.2018");
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
			if (res != null) res.close();
		}
	}

	private static Qname getEvaluationId(HttpClientService client) throws ClientProtocolException, IOException {
		return new Qname(client.contentAsJson(new HttpGet("https://triplestore.luomus.fi/uri/MKV")).getObject("response").getString("qname"));
	}

	private static Set<Qname> taxonIds() {
		Set<Qname> taxonIds = new HashSet<>();
		taxonIds.add(new Qname("MX.66198"));
		return taxonIds;
	}

}
