package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public String indent(Qname qname) {
		return labels.get(qname);
	}

	public int indentCount(Qname qname) {
		return indents.get(qname);
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
		for (RdfProperty p : properties) {
			List<RdfProperty> parents = getParentChain(p, properties);
			indents.put(p.getQname(), parents.size());
		}
		return indents;
	}

	private List<RdfProperty> getParentChain(RdfProperty p, Collection<RdfProperty> properties) {
		List<RdfProperty> parents = new ArrayList<>();
		if (p.getAltParent() != null) {
			RdfProperty parent = getProperty(p.getAltParent(), properties);
			if (parent != null) {
				parents.add(parent);
				parents.addAll(getParentChain(parent, properties));
			}
		}
		return parents;
	}

	private RdfProperty getProperty(Qname altParent, Collection<RdfProperty> properties) {
		for (RdfProperty p : properties) {
			if (p.getQname().equals(altParent)) return p;
		}
		return null;
	}

}
