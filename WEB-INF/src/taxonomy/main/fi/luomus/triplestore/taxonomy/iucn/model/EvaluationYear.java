package fi.luomus.triplestore.taxonomy.iucn.model;

public class EvaluationYear {

	private final int year;
	private final boolean locked;

	public EvaluationYear(int year, boolean locked) {
		this.year = year;
		this.locked = locked;
	}

	public int getYear() {
		return year;
	}

	public boolean isLocked() {
		return locked;
	}

}
