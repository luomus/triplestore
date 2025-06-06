package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.taxonomy.iucn.Evaluation;

public class YearlyGroupStat {

	private final Container iucnContainer;
	private final int year;
	private final String groupQname;
	private Integer readyCount = null;
	private Integer startedCount = null;

	public YearlyGroupStat(int year, String groupQname, Container iucnContainer) {
		this.iucnContainer = iucnContainer;
		this.year = year;
		this.groupQname = groupQname;
	}

	public void invalidate() {
		readyCount = null;
		startedCount = null;
	}

	public int getReadyCount() throws Exception {
		if (readyCount != null) return readyCount;
		readyCount = countReady();
		return readyCount;
	}

	private int countReady() throws Exception {
		int count = 0;
		for (EvaluationTarget target : iucnContainer.getTargetsOfGroup(groupQname)) {
			Evaluation evaluation = target.getEvaluation(year);
			if (evaluation == null) continue;
			if (evaluation.isReady()) count++;
		}
		return count;
	}

	public int getStartedCount() throws Exception {
		if (startedCount != null) return startedCount;
		startedCount = countStarted();
		return startedCount;
	}

	private int countStarted() throws Exception {
		int count = 0;
		for (EvaluationTarget target : iucnContainer.getTargetsOfGroup(groupQname)) {
			Evaluation evaluation = target.getEvaluation(year);
			if (evaluation == null) continue;
			if (evaluation.isStarted()) count++;
			if (evaluation.isReadyForComments()) count++;
		}
		return count;
	}
	
	public int getSpeciesOfGroupCount() throws Exception {
		return iucnContainer.getTargetsOfGroup(groupQname).size();
	}
	
}
