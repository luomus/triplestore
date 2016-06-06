package fi.luomus.triplestore.taxonomy.models;

public class IUCNYearlyGroupStat {

	private final IUCNContainer iucnContainer;
	private final int year;
	private final String groupQname;
	private Integer readyCount = null;

	public IUCNYearlyGroupStat(int year, String groupQname, IUCNContainer iucnContainer) {
		this.iucnContainer = iucnContainer;
		this.year = year;
		this.groupQname = groupQname;
	}

	public void invalidate() {
		readyCount = null;
	}

	public int getReadyCount() throws Exception {
		if (readyCount != null) return readyCount;
		readyCount = count();
		return readyCount;
	}

	private int count() throws Exception {
		int count = 0;
		for (IUCNEvaluationTarget target : iucnContainer.getTargetsOfGroup(groupQname)) {
			IUCNEvaluation evaluation = target.getEvaluation(year);
			if (evaluation == null) continue;
			if (evaluation.isReady()) count++;
		}
		return count;
	}

	public int getSpeciesOfGroupCount() throws Exception {
		return iucnContainer.getTargetsOfGroup(groupQname).size();
	}
	
}
