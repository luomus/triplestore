package fi.luomus.triplestore.models;

import java.util.ArrayList;
import java.util.List;

public class History {

	private static final int MAX_SIZE = 30;
	private List<String> history = new ArrayList<>();

	public void visited(String qname) {
		history.remove(qname);
		history.add(0, qname);
		if (history.size() > MAX_SIZE) {
			history = new ArrayList<>(history.subList(0, MAX_SIZE));
		}
	}

	public List<String> getPrevious(int count) {
		count = Math.min(count, history.size());
		return history.subList(0, count);
	}

}
