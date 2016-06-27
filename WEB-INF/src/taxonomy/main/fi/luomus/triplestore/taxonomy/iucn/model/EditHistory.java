package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.List;

public class EditHistory {

	public static class EditHistoryEntry {
		private final String notes;
		private final String editorQname;
		public EditHistoryEntry(String notes, String editorQname) {
			this.notes = notes;
			this.editorQname = validEditor(editorQname); 
		}
		private String validEditor(String editorQname) {
			if (editorQname == null) return null;
			if (!editorQname.startsWith("MA.")) return null;
			return editorQname;
		}
		public String getNotes() {
			return notes;
		}
		public String getEditorQname() {
			return editorQname;
		}
	}

	private final List<EditHistoryEntry> entries = new ArrayList<>();

	public void add(EditHistoryEntry entry) {
		entries.add(entry);
	}

	public List<EditHistoryEntry> getEntries() {
		return entries;
	}

}
