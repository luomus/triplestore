package fi.luomus.triplestore.models;

public class CreatableResource {

	private final String namespacePrefix;
	private final String description;
	private final String type;
	
	public CreatableResource(String namespacePrefix, String description, String type) {
		this.namespacePrefix = namespacePrefix;
		this.description = description;
		this.type = type;
	}
	
	public String getNamespacePrefix() {
		return namespacePrefix;
	}
	
	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}
		
}
