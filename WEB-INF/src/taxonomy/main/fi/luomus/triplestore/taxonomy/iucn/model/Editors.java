package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Qname;

import java.util.ArrayList;
import java.util.List;

public class Editors {

	private final Qname id;
	private final List<Qname> editors = new ArrayList<>();

	public Editors(Qname id) {
		this.id = id;
	}

	public Qname getId() {
		return id;
	}

	public List<Qname> getEditors() {
		return editors;
	}

	public void addEditor(Qname editor) {
		this.editors.add(editor);
	}
	
}
