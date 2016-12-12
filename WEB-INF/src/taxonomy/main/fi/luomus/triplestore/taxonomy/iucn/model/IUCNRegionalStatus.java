package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Qname;

public class IUCNRegionalStatus {

	private Qname id;
	private Qname area;
	private Boolean status;
	
	public IUCNRegionalStatus(Qname id, Qname area, Boolean status) {
		this.id = id;
		this.area = area;
		this.status = status;
	}
	
	public Qname getArea() {
		return area;
	}
	public void setArea(Qname area) {
		this.area = area;
	}
	public Boolean getStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	public Qname getId() {
		return id;
	}
	public void setId(Qname id) {
		this.id = id;
	}

}
