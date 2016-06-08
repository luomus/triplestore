package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Qname;

import java.util.ArrayList;
import java.util.List;

public class IUCNEditors {

	private final Qname id;
	private final List<Qname> editors = new ArrayList<>();

	public IUCNEditors(Qname id) {
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
