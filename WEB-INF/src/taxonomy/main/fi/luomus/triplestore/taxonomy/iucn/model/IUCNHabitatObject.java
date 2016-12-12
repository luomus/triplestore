package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.Utils;

public class IUCNHabitatObject implements Comparable<IUCNHabitatObject> {

	private Qname id;
	private final String habitat;
	private final int order;
	private List<String> habitatSpecificTypes = null;

	public IUCNHabitatObject(Qname id, String habitat, int order) {
		this.id = id;
		this.habitat = habitat;
		this.order = order;
	}

	public void addHabitatSpecificType(String type) {
		if (habitatSpecificTypes == null) habitatSpecificTypes = new ArrayList<>();
		habitatSpecificTypes.add(type);
	}

	public List<String> getHabitatSpecificTypes() {
		if (habitatSpecificTypes == null) return Collections.emptyList();
		return Collections.unmodifiableList(habitatSpecificTypes);
	}

	public Qname getId() {
		return id;
	}

	public String getHabitat() {
		return habitat;
	}

	@Override
	public String toString() {
		return Utils.debugS(id, habitat, habitatSpecificTypes);
	}

	public void setId(Qname id) {
		this.id = id;
	}

	public boolean hasValues() {
		if (given(getHabitat())) return true;
		for (String habitat : getHabitatSpecificTypes()) {
			if (given(habitat)) return true;
		}
		return false;
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public int compareTo(IUCNHabitatObject o) {
		return Integer.valueOf(this.order).compareTo(o.order);
	}

}
