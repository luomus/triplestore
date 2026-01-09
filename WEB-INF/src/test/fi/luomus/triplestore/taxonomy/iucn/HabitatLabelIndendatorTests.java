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
	public void indenting() {
		Collection<RdfProperty> properties = new ArrayList<>();
		properties.add(new RdfProperty(Qname.of("1")).setLabels(new LocalizedText().set("fi",  "A - xxx")));
		properties.add(new RdfProperty(Qname.of("2")).setLabels(new LocalizedText().set("fi",  "B - xxx")));
		properties.add(new RdfProperty(Qname.of("3")).setLabels(new LocalizedText().set("fi",  "C - xxx")));
		properties.add(new RdfProperty(Qname.of("4")).setLabels(new LocalizedText().set("fi",  "C1 - xxx")).setAltParent(Qname.of("3")));
		properties.add(new RdfProperty(Qname.of("5")).setLabels(new LocalizedText().set("fi",  "C2")).setAltParent(Qname.of("3")));
		properties.add(new RdfProperty(Qname.of("6")).setLabels(new LocalizedText().set("fi",  "C2A")).setAltParent(Qname.of("5")));
		properties.add(new RdfProperty(Qname.of("7")).setLabels(new LocalizedText().set("fi",  "C2B")).setAltParent(Qname.of("5")));
		properties.add(new RdfProperty(Qname.of("8")).setLabels(new LocalizedText().set("fi",  "C3")).setAltParent(Qname.of("3")));
		properties.add(new RdfProperty(Qname.of("9")).setLabels(new LocalizedText().set("fi",  "C4")).setAltParent(Qname.of("3")));
		properties.add(new RdfProperty(Qname.of("10")).setLabels(new LocalizedText().set("fi", "C4A")).setAltParent(Qname.of("9")));
		properties.add(new RdfProperty(Qname.of("11")).setLabels(new LocalizedText().set("fi", "C4Aa")).setAltParent(Qname.of("10")));
		properties.add(new RdfProperty(Qname.of("12")).setLabels(new LocalizedText().set("fi", "C4Ab")).setAltParent(Qname.of("10")));
		properties.add(new RdfProperty(Qname.of("13")).setLabels(new LocalizedText().set("fi", "C4B")).setAltParent(Qname.of("9")));
		properties.add(new RdfProperty(Qname.of("14")).setLabels(new LocalizedText().set("fi", "C4Ba")).setAltParent(Qname.of("13")));	
		properties.add(new RdfProperty(Qname.of("15")).setLabels(new LocalizedText().set("fi", "C4Ba1")).setAltParent(Qname.of("14")));
		properties.add(new RdfProperty(Qname.of("16")).setLabels(new LocalizedText().set("fi", "C4Ba2")).setAltParent(Qname.of("14")));
		properties.add(new RdfProperty(Qname.of("17")).setLabels(new LocalizedText().set("fi", "C5")).setAltParent(Qname.of("3")));
		
		HabitatLabelIndendator indentator = new HabitatLabelIndendator(properties);
		assertEquals(0, indentator.indentCount(Qname.of("1")));
		assertEquals(0, indentator.indentCount(Qname.of("2")));
		assertEquals(0, indentator.indentCount(Qname.of("3")));
		assertEquals(1, indentator.indentCount(Qname.of("4")));
		assertEquals(1, indentator.indentCount(Qname.of("5")));
		assertEquals(2, indentator.indentCount(Qname.of("6")));
		assertEquals(2, indentator.indentCount(Qname.of("7")));
		assertEquals(1, indentator.indentCount(Qname.of("8")));
		assertEquals(1, indentator.indentCount(Qname.of("9")));
		assertEquals(2, indentator.indentCount(Qname.of("10")));
		assertEquals(3, indentator.indentCount(Qname.of("11")));
		assertEquals(3, indentator.indentCount(Qname.of("12")));
		assertEquals(2, indentator.indentCount(Qname.of("13")));
		assertEquals(3, indentator.indentCount(Qname.of("14")));
		assertEquals(4, indentator.indentCount(Qname.of("15")));
		assertEquals(4, indentator.indentCount(Qname.of("16")));
		assertEquals(1, indentator.indentCount(Qname.of("17")));
		
		assertEquals("A - xxx", indentator.indent(Qname.of("1")));
		assertEquals("&nbsp;&nbsp; C1 - xxx", indentator.indent(Qname.of("4")));
		assertEquals("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; C4Ba1", indentator.indent(Qname.of("15")));
	}
	
}
