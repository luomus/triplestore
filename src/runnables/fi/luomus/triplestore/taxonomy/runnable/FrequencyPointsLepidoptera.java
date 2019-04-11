package fi.luomus.triplestore.taxonomy.runnable;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpGet;

import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.commons.utils.URIBuilder;

public class FrequencyPointsLepidoptera {

	private static HttpClientService client;

	public static void main(String[] args) {
		try {
			client = new HttpClientService();
			generateStatements();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (client != null) client.close();
		}
		System.out.println("done");
	}

	private static void generateStatements() throws Exception {
		for (String line : FileUtils.readLines(new File("C:/temp/frekvenssit/perhoset.txt"))) {
			if (line.trim().isEmpty()) continue;
			String[] parts = line.split(Pattern.quote("\t"));
			generateStatements(parts);
		}
	}

	private static String genus = "";

	private static void generateStatements(String[] parts) throws Exception {
		String taxon = FrequencyPointsFromMXCodeList.val(parts, 0);
		if (taxon.equals("ssp.")) return;
		if (taxon.equals("f.")) return;
		
		if (!taxon.startsWith("MX.") && startsWithUpperCase(taxon)) {
			genus = taxon;
			return;
		}

		String points = FrequencyPointsFromMXCodeList.val(parts, 1);
		if (points == null) {
			return;
		}

		String taxonId = taxon.startsWith("MX.") ? taxon : taxonId(genus, taxon);
		if (taxonId == null) return;

		points = points.replace("R,", "").replace(" ", "");
		if (points.contains(",")) points = points.split(Pattern.quote(","))[0];
		FrequencyPointsFromMXCodeList.generateStatements(taxonId, points, null);
	}

	private static boolean startsWithUpperCase(String species) {
		return Character.isUpperCase(species.charAt(0));
	}

	private static String taxonId(String genus, String species) throws Exception {
		String sciname = genus + " " + species;
		URIBuilder uri = new URIBuilder("https://laji.fi/api/taxa/search")
				.addParameter("query", sciname)
				.addParameter("informalTaxonGroup", "MVL.31")
				.addParameter("matchType", "exact");

		JSONArray response = client.contentAsJsonArray(new HttpGet(uri.getURI()));
		if (response.size() == 0) {
			System.out.println("XXX NOT FOUND " + sciname);
			return null;
		}
		if (response.size() > 1) {
			System.out.println("XXX MULTI FOUND " + sciname);
			return null;
		}
		return response.iterateAsObject().get(0).getString("id");
	}

}
