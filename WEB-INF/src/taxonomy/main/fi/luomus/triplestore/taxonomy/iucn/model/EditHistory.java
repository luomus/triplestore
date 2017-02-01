package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EditHistory {

	public static class EditHistoryEntry {
		private static final int DATE_LENGTH = "dd.mm.yyyy".length();
		private final String notes;
		private final String date;
		private final String editorQname;
		public EditHistoryEntry(String notes, String editorQname) {
			this.date = exctractDate(notes);
			this.notes = date == null ? notes : notes.replace(IUCNEvaluation.NOTE_DATE_SEPARATOR+date, "");
			this.editorQname = validEditor(editorQname); 
		}
		private String exctractDate(String notes) {
			if (notes == null) return null;
			String[] parts =  notes.split(Pattern.quote(IUCNEvaluation.NOTE_DATE_SEPARATOR));
			if (parts.length < 2) return null;
			String date = parts[parts.length-1].trim();
			if (isDate(date)) return date;
			return null;
		}
		private boolean isDate(String s) {
			if (s.length() != DATE_LENGTH) return false;
			if (!Character.isDigit(s.charAt(0))) return false;
			if (Utils.countNumberOf(".", s) != 2) return false;
			return true;
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
		public String getDate() {
			return date;
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
