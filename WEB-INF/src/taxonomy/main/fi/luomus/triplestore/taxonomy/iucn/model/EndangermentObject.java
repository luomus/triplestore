package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Qname;

public class EndangermentObject implements Comparable<EndangermentObject> {

	public Qname id;
	public final Qname endangerment;
	public final int order;
	
	public EndangermentObject(Qname id, Qname endangerment, int order) {
		this.id = id;
		this.endangerment = endangerment;
		this.order = order;
	}

	public Qname getId() {
		return id;
	}

	public Qname getEndangerment() {
		return endangerment;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public int compareTo(EndangermentObject o) {
		return Integer.valueOf(order).compareTo(o.order);
	}

	public void setId(Qname id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return endangerment.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass())
			return false;
		EndangermentObject other = (EndangermentObject) obj;
		return this.endangerment.equals(other.endangerment);
	}
	
	@Override
	public String toString() {
		return this.endangerment.toString();
	}
	
}
