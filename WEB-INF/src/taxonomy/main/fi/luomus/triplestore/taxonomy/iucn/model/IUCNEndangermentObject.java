package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Qname;

public class IUCNEndangermentObject implements Comparable<IUCNEndangermentObject> {

	public Qname id;
	public final Qname endangerment;
	public final int order;
	
	public IUCNEndangermentObject(Qname id, Qname endangerment, int order) {
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
	public int compareTo(IUCNEndangermentObject o) {
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
		IUCNEndangermentObject other = (IUCNEndangermentObject) obj;
		return this.endangerment.equals(other.endangerment);
	}
	
}
