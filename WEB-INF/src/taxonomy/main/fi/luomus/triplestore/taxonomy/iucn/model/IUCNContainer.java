package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.triplestore.taxonomy.dao.IucnDAOImple;

public class IUCNContainer {

	private static final int DATE_LENGTH = "dd.mm.yyyy:".length();

	private static final Object LOCK = new Object();

	private final IucnDAOImple iucnDAO;
	private final Map<Integer, Map<String, IUCNYearlyGroupStat>> stats = new HashMap<>();
	private final Map<String, List<IUCNEvaluationTarget>> targetsOfGroup = new HashMap<>();
	private final Map<String, List<String>> groupsOfTarget = new HashMap<>();
	private final Map<String, IUCNEvaluationTarget> targets = new HashMap<>();
	private final Map<String, TreeSet<Remark>> remarksOfGroup = new HashMap<>();

	public IUCNContainer(IucnDAOImple iucnDAO) {
		this.iucnDAO = iucnDAO;
	}

	public Collection<IUCNEvaluationTarget> getTargets() {
		List<IUCNEvaluationTarget> t = new ArrayList<>(targets.values());
		return Collections.unmodifiableCollection(t);
	}

	public IUCNEvaluationTarget getTarget(String speciesQname) throws Exception {
		if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
		synchronized (LOCK) {
			if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
			return iucnDAO.loadTarget(speciesQname);
		}
	}

	public IUCNContainer addTarget(IUCNEvaluationTarget target) {
		targets.put(target.getQname(), target);
		return this;
	}

	public boolean hasTarget(String speciesQname) {
		return targets.containsKey(speciesQname);
	}

	public List<IUCNEvaluationTarget> getTargetsOfGroup(String groupQname) throws Exception {
		if (targetsOfGroup.containsKey(groupQname)) return targetsOfGroup.get(groupQname);
		synchronized (LOCK) {
			if (targetsOfGroup.containsKey(groupQname)) return targetsOfGroup.get(groupQname);
			List<IUCNEvaluationTarget> targets = new ArrayList<>();
			for (String speciesQname : iucnDAO.loadSpeciesOfGroup(groupQname)) {
				targets.add(getTarget(speciesQname));
				if (!groupsOfTarget.containsKey(speciesQname)) {
					groupsOfTarget.put(speciesQname, new ArrayList<String>());
				}
				if (!groupsOfTarget.get(speciesQname).contains(groupQname)) {
					groupsOfTarget.get(speciesQname).add(groupQname);
				}
			}
			targetsOfGroup.put(groupQname, Collections.unmodifiableList(targets));
			return targetsOfGroup.get(groupQname);
		}
	}

	public List<String> getGroupsOfTarget(String speciesQname) {
		synchronized (LOCK) {
			if (!groupsOfTarget.containsKey(speciesQname)) return Collections.emptyList();
			return Collections.unmodifiableList(groupsOfTarget.get(speciesQname));
		}
	}

	public IUCNYearlyGroupStat getStat(int year, String groupQname) {
		synchronized (LOCK) {
			if (!stats.containsKey(year)) {
				stats.put(year, new HashMap<String, IUCNYearlyGroupStat>());
			}
			if (!stats.get(year).containsKey(groupQname)) {
				stats.get(year).put(groupQname, new IUCNYearlyGroupStat(year, groupQname, this));
			}
			return stats.get(year).get(groupQname);
		}
	}

	public void setEvaluation(IUCNEvaluation evaluation) throws Exception {
		synchronized (LOCK) {
			String speciesQname = evaluation.getSpeciesQname();
			IUCNEvaluationTarget target = getTarget(speciesQname);
			setEvaluation(evaluation, target);
		}
	}

	private void setEvaluation(IUCNEvaluation evaluation, IUCNEvaluationTarget target) {
		target.setEvaluation(evaluation);
		invalidateStats(evaluation, target);
	}

	private void invalidateStats(IUCNEvaluation evaluation, IUCNEvaluationTarget target) {
		for (String groupQname : target.getGroups()) {
			getStat(evaluation.getEvaluationYear(), groupQname).invalidate();
		}
	}

	public void moveEvaluation(IUCNEvaluation evaluation, IUCNEvaluationTarget from, IUCNEvaluationTarget to) {
		synchronized (LOCK) {
			from.removeEvaluation(evaluation);
			setEvaluation(evaluation, to);
			invalidateStats(evaluation, from);
			invalidateStats(evaluation, to);
		}
	}

	public void deleteEvaluation(IUCNEvaluation evaluation, IUCNEvaluationTarget from) {
		synchronized (LOCK) {
			from.removeEvaluation(evaluation);
			invalidateStats(evaluation, from);
		}
	}

	public Collection<Remark> getRemarksForGroup(String groupQname) throws Exception {
		if (remarksOfGroup.containsKey(groupQname)) return remarksOfGroup.get(groupQname);
		synchronized (LOCK) {
			if (remarksOfGroup.containsKey(groupQname)) return remarksOfGroup.get(groupQname);
			TreeSet<Remark> remarks = new TreeSet<>();
			for (IUCNEvaluationTarget target : getTargetsOfGroup(groupQname)) {
				for (IUCNEvaluation evaluation : target.getEvaluations()) {
					if (evaluation.hasRemarks()) {
						addRemarks(generateRemarks(evaluation, target), remarks);
					}
				}
			}
			remarksOfGroup.put(groupQname, remarks);
			return remarks;
		}
	}

	private Collection<Remark> generateRemarks(IUCNEvaluation evaluation, IUCNEvaluationTarget target) {
		ArrayList<Remark> remarks = new ArrayList<>();
		for (Statement statement : evaluation.getRemarkSatements()) {
			String remarkContents = statement.getObjectLiteral().getContent();
			try {
				String date = getDatePart(remarkContents);
				String remark = getRemarkPart(remarkContents);
				String personName = getPersonNamePart(remarkContents);
				remarks.add(new Remark(statement.getId(), target, date, remark, personName));
			} catch (Exception e) {
				new Exception(remarkContents, e).printStackTrace();
			}
		}
		return remarks;
	}

	private String getPersonNamePart(String remark) {
		//		Esko Piirainen 26.01.2017:
		//		Testikommentti!
		//		Usealle riville...
		String s = remark.split(Pattern.quote("\n"))[0];
		return s.substring(0, s.length() - DATE_LENGTH);
	}

	private String getDatePart(String remark) {
		String s = remark.split(Pattern.quote("\n"))[0];
		return s.substring(s.length() - DATE_LENGTH, s.length()-1);
	}

	private String getRemarkPart(String remark) {
		StringBuilder b = new StringBuilder();
		int i = 0;
		for (String line : remark.split(Pattern.quote("\n"))) {
			if (i++ == 0) continue;
			b.append(line).append("\n");
		}
		return b.toString();
	}

	private void addRemarks(Collection<Remark> newRemarks, Collection<Remark> existingRemarks) {
		existingRemarks.addAll(newRemarks);
		while (existingRemarks.size() > 20) {
			Iterator<Remark> i = existingRemarks.iterator();
			i.next();
			i.remove();
		}
	}

	public void removeRemark(IUCNEvaluationTarget target, int deletedId) throws Exception {
		for (String groupQname : target.getGroups()) {
			Iterator<Remark> i = getRemarksForGroup(groupQname).iterator();
			while (i.hasNext()) {
				Remark remark = i.next();
				if (remark.getId() == deletedId) {
					i.remove();
					return;
				}
			}
		}
	}

	public void addRemark(IUCNEvaluationTarget target, IUCNEvaluation evaluation) throws Exception {
		for (String groupQname : target.getGroups()) {
			addRemarks(generateRemarks(evaluation, target), getRemarksForGroup(groupQname));
		}
	}

	public void complateLoading(IUCNEvaluation evaluation) throws Exception {
		if (evaluation.isIncompletelyLoaded()) {
			synchronized (LOCK) {
				if (evaluation.isIncompletelyLoaded()) {
					iucnDAO.completeLoading(evaluation);
				}
			}
		}
	}

}
