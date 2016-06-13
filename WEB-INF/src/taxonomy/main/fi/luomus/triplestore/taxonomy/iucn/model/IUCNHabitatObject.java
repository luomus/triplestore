package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IUCNHabitatObject {

	private final String id;
	private final String habitat;
	private List<String> habitatSpecificTypes = null;

	public IUCNHabitatObject(String id, String habitat) {
		this.id = id;
		this.habitat = habitat;
	}

	public void addHabitatSpecificType(String type) {
		if (habitatSpecificTypes == null) habitatSpecificTypes = new ArrayList<>();
		habitatSpecificTypes.add(type);
	}

	public List<String> getHabitatSpecificTypes() {
		if (habitatSpecificTypes == null) return Collections.emptyList();
		return Collections.unmodifiableList(habitatSpecificTypes);
	}

	public String getId() {
		return id;
	}

	public String getHabitat() {
		return habitat;
	}

}
