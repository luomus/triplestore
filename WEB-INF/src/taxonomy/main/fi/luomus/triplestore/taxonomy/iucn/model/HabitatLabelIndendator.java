package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;

public class HabitatLabelIndendator {

	private static final String INDENT = "&nbsp;&nbsp;";
	private final Map<Qname, Integer> indents;
	private final Map<Qname, String> labels;

	public HabitatLabelIndendator(Collection<RdfProperty> properties) {
		this.labels = new HashMap<>();
		indents = resolveIndents(properties);
		for (RdfProperty p : properties) {
			labels.put(p.getQname(), doIndent(p));
		}
	}

	private String doIndent(RdfProperty p) {
		int indent = indents.get(p.getQname());
		String label = p.getLabel().forLocale("fi");
		if (indent == 0) return label;
		label = " " + label;
		for (int i = indent; i>0; i--) {
			label = INDENT + label;
		}
		return label;
	}

	private Map<Qname, Integer> resolveIndents(Collection<RdfProperty> properties) {
		Map<Qname, Integer> indents = new HashMap<>();
		String prevLabelPrefix = "/|/|/|/|/|/|";
		for (RdfProperty p : properties) {
			String thisLabelPrefix = getLabelPrefix(p);
			int countOfSameLettersInBeginning = getCountOfSameLettersInBeginning(thisLabelPrefix, prevLabelPrefix);
			indents.put(p.getQname(), countOfSameLettersInBeginning);
			prevLabelPrefix = thisLabelPrefix;
		}
		return indents;
	}

	private int getCountOfSameLettersInBeginning(String s1, String s2) {
		int count = 0;
		int i = 0;
		for (char c1 : s1.toCharArray()) {
			try {
				char c2 = s2.charAt(i++);
				if (c2 != c1) return count;
				count++;
			} catch (IndexOutOfBoundsException e) {
				return count;
			}
		}
		return count;
	}

	private String getLabelPrefix(RdfProperty p) {
		return p.getLabel().forLocale("fi").split(Pattern.quote(" "))[0];
	}

	public String indent(Qname qname) {
		return labels.get(qname);
	}

	public int indentCount(Qname qname) {
		return indents.get(qname);
	}


}
