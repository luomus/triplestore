package fi.luomus.triplestore.utils;

import fi.luomus.commons.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Assert;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaUtils {

	public static Model newDefaultModel() {
		Model jenaModel = ModelFactory.createDefaultModel();
		return jenaModel;
	}

	public static Model convert(String rdf) {
		Model jenaModel = JenaUtils.newDefaultModel();
		InputStream is = new ByteArrayInputStream(rdf.getBytes());
		jenaModel.read(is, null, "RDF/XML-ABBREV");
		return jenaModel;
	}

	public static void assertEquals(String expected, Model jenaModel) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		jenaModel.write(os, "RDF/XML-ABBREV");
		Assert.assertEquals(Utils.removeWhitespace(expected), Utils.removeWhitespace(os.toString()));
	}

	public static void debugRdf(Model jenaModel) {
		debugRdf(jenaModel, "RDF/XML-ABBREV");
	}

	public static void debugRdf(Model jenaModel, String language) {
		jenaModel.write(System.out, language);
		System.out.println();
	}

	public static String getRdf(Model jenaModel) {
		return getRdf(jenaModel, "RDF/XML-ABBREV");
	}

	public static String getRdf(Model jenaModel, String language) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		jenaModel.write(os, language);
		String rdfXml = os.toString();
		String sortedXml = sort(rdfXml); 
		return sortedXml;
	}

	private static String sort(String rdfXml) {
//		Document doc = new XMLReader().parse(rdfXml);
//		List<Node> childs = doc.getRootNode().getChildNodes();
//		Collections.sort(childs, new Comparator<Node>() {
//			@Override
//			public int compare(Node n1, Node n2) {
//				Qname q1 = Qname.fromURI(n1.getAttribute("rdf:about"));
//				Qname q2 = Qname.fromURI(n2.getAttribute("rdf:about"));
//				return q1.compareTo(q2);
//			}
//		});
//		String orderedXml = new XMLWriter(doc).generateXML();
//		return orderedXml; // TODO XMLWriter muuttaa sisällön entiteeteiksi joka ei oo hyvä 
		return rdfXml;
	}

	public static void debugStatements(Model jenaModel) {
		if (jenaModel == null) return;
		StringBuilder b = new StringBuilder();
		StmtIterator iterator = jenaModel.listStatements();
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			b.append(subject.getURI()).append(" \t->\t ");
			b.append(predicate.toString()).append(" \t->\t ");
			if (object instanceof Resource) {
				b.append(((Resource) object).getURI());
			} else {
				b.append(object.asLiteral().getString()).append("\t");
				b.append("@").append(object.asLiteral().getLanguage());
			}
			b.append("\n");
		}
		System.out.println(b.toString());
	}

}
