package fi.luomus.triplestore.taxonomy.iucn.model;

public class Remark implements Comparable<Remark> {

	private final int id;
	private final EvaluationTarget target;
	private final String date;
	private final String remark;
	private final String personName;
	
	public Remark(int id, EvaluationTarget target, String date, String remark, String personName) {
		this.id = id;
		this.target = target;
		this.date = date;
		this.remark = remark;
		this.personName = personName;
		if (id <= 0) throw new IllegalStateException("Id is " + id);
	}

	public EvaluationTarget getTarget() {
		return target;
	}

	public String getDate() {
		return date;
	}

	public String getRemark() {
		return remark;
	}

	public String getShortenedRemark() {
		if (remark.length() > 40) {
			return remark.substring(0, 39) + "...";
		}
		return remark;
	}
	
	public String getPersonName() {
		return personName;
	}

	public int getId() {
		return id;
	}

	@Override
	public int compareTo(Remark o) {
		return Integer.valueOf(id).compareTo(o.getId());
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Remark other = (Remark) obj;
		if (id != other.id) return false;
		return true;
	}
		
}
