package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Qname;

import java.util.ArrayList;
import java.util.List;

public class TaxonGroupIucnEditors {

	private final Qname id;
	private final Qname taxonGroup;
	private final List<Qname> editors = new ArrayList<>();
	
	public TaxonGroupIucnEditors(Qname id, Qname taxonGroup) {
		this.id = id;
		this.taxonGroup = taxonGroup;
	}

	public Qname getId() {
		return id;
	}

	public Qname getTaxonGroup() {
		return taxonGroup;
	}

	public List<Qname> getEditors() {
		return editors;
	}
	
	public TaxonGroupIucnEditors addEditor(Qname editor) {
		editors.add(editor);
		return this;
	}
	
}
