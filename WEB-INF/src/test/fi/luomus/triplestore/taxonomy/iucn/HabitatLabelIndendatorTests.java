package fi.luomus.triplestore.taxonomy.iucn;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.triplestore.taxonomy.iucn.model.HabitatLabelIndendator;

public class HabitatLabelIndendatorTests {
	
	@Test
	public void indenting() throws Exception {
		Collection<RdfProperty> properties = new ArrayList<>();
		properties.add(new RdfProperty(new Qname("1")).setLabels(new LocalizedText().set("fi",  "A - xxx")));
		properties.add(new RdfProperty(new Qname("2")).setLabels(new LocalizedText().set("fi",  "B - xxx")));
		properties.add(new RdfProperty(new Qname("3")).setLabels(new LocalizedText().set("fi",  "C - xxx")));
		properties.add(new RdfProperty(new Qname("4")).setLabels(new LocalizedText().set("fi",  "C1 - xxx")));
		properties.add(new RdfProperty(new Qname("5")).setLabels(new LocalizedText().set("fi",  "C2")));
		properties.add(new RdfProperty(new Qname("6")).setLabels(new LocalizedText().set("fi",  "C2A")));
		properties.add(new RdfProperty(new Qname("7")).setLabels(new LocalizedText().set("fi",  "C2B")));
		properties.add(new RdfProperty(new Qname("8")).setLabels(new LocalizedText().set("fi",  "C3")));
		properties.add(new RdfProperty(new Qname("9")).setLabels(new LocalizedText().set("fi",  "C4")));
		properties.add(new RdfProperty(new Qname("10")).setLabels(new LocalizedText().set("fi", "C4A")));
		properties.add(new RdfProperty(new Qname("11")).setLabels(new LocalizedText().set("fi", "C4Aa")));
		properties.add(new RdfProperty(new Qname("12")).setLabels(new LocalizedText().set("fi", "C4Ab")));
		properties.add(new RdfProperty(new Qname("13")).setLabels(new LocalizedText().set("fi", "C4B")));
		properties.add(new RdfProperty(new Qname("14")).setLabels(new LocalizedText().set("fi", "C4Ba")));	
		properties.add(new RdfProperty(new Qname("15")).setLabels(new LocalizedText().set("fi", "C4Ba1")));
		properties.add(new RdfProperty(new Qname("16")).setLabels(new LocalizedText().set("fi", "C4Ba2")));
		properties.add(new RdfProperty(new Qname("17")).setLabels(new LocalizedText().set("fi", "C5")));
		
		HabitatLabelIndendator indentator = new HabitatLabelIndendator(properties);
		assertEquals(0, indentator.indentCount(new Qname("1")));
		assertEquals(0, indentator.indentCount(new Qname("2")));
		assertEquals(0, indentator.indentCount(new Qname("3")));
		assertEquals(1, indentator.indentCount(new Qname("4")));
		assertEquals(1, indentator.indentCount(new Qname("5")));
		assertEquals(2, indentator.indentCount(new Qname("6")));
		assertEquals(2, indentator.indentCount(new Qname("7")));
		assertEquals(1, indentator.indentCount(new Qname("8")));
		assertEquals(1, indentator.indentCount(new Qname("9")));
		assertEquals(2, indentator.indentCount(new Qname("10")));
		assertEquals(3, indentator.indentCount(new Qname("11")));
		assertEquals(3, indentator.indentCount(new Qname("12")));
		assertEquals(2, indentator.indentCount(new Qname("13")));
		assertEquals(3, indentator.indentCount(new Qname("14")));
		assertEquals(4, indentator.indentCount(new Qname("15")));
		assertEquals(4, indentator.indentCount(new Qname("16")));
		assertEquals(1, indentator.indentCount(new Qname("17")));
		
		assertEquals("A - xxx", indentator.indent(new Qname("1")));
		assertEquals("&nbsp;&nbsp; C1 - xxx", indentator.indent(new Qname("4")));
		assertEquals("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; C4Ba1", indentator.indent(new Qname("15")));
	}
	
}
