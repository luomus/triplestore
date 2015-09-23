package fi.luomus.triplestore.models;

public class ResourceListing {

	private final String name;
	private final int count;
	
	public ResourceListing(String name, int count) {
		this.name = name;
		this.count = count;
	}
	
	public String getName() {
		return name;
	}
	
	public int getCount() {
		return count;
	}

}
