package fi.luomus.triplestore.taxonomy.iucn.model;

public class IUCNYearlyGroupStat {

	private final IUCNContainer iucnContainer;
	private final int year;
	private final String groupQname;
	private Integer readyCount = null;
	private Integer startedCount = null;

	public IUCNYearlyGroupStat(int year, String groupQname, IUCNContainer iucnContainer) {
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
		for (IUCNEvaluationTarget target : iucnContainer.getTargetsOfGroup(groupQname)) {
			IUCNEvaluation evaluation = target.getEvaluation(year);
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
		for (IUCNEvaluationTarget target : iucnContainer.getTargetsOfGroup(groupQname)) {
			IUCNEvaluation evaluation = target.getEvaluation(year);
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
