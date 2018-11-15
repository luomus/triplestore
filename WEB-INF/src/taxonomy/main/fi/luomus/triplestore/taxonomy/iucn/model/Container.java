package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.triplestore.taxonomy.dao.IucnDAOImple;

public class Container {

	private static final int DATE_LENGTH = "dd.mm.yyyy:".length();

	private static final Object LOCK = new Object();

	private final IucnDAOImple iucnDAO;
	private final Map<Integer, Map<String, YearlyGroupStat>> stats = new HashMap<>();
	private final Map<String, List<EvaluationTarget>> targetsOfGroup = new LinkedHashMap<>();
	private final Map<String, List<String>> groupsOfTarget = new HashMap<>();
	private final Map<String, EvaluationTarget> targets = new HashMap<>();
	private final Map<String, TreeSet<Remark>> remarksOfGroup = new HashMap<>();

	public Container(IucnDAOImple iucnDAO) {
		this.iucnDAO = iucnDAO;
	}

	public Collection<EvaluationTarget> getGroupOrderedTargets() {
		List<EvaluationTarget> t = new ArrayList<>(60000);
		for (List<EvaluationTarget> groupTargets : targetsOfGroup.values()) {
			t.addAll(groupTargets);
		}
		return Collections.unmodifiableCollection(t);
	}
	public Collection<EvaluationTarget> getTargets() {
		List<EvaluationTarget> t = new ArrayList<>(targets.values());
		return Collections.unmodifiableCollection(t);
	}

	public Container makeSureEvaluationDataIsLoaded() throws Exception {
		iucnDAO.makeSureEvaluationDataIsLoaded();
		return this;
	}
	
	public EvaluationTarget getTarget(String speciesQname) throws Exception {
		if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
		synchronized (LOCK) {
			if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
			return iucnDAO.loadTarget(speciesQname);
		}
	}

	public Container addTarget(EvaluationTarget target) {
		targets.put(target.getQname(), target);
		return this;
	}

	public boolean hasTarget(String speciesQname) {
		return targets.containsKey(speciesQname);
	}

	public List<EvaluationTarget> getTargetsOfGroup(String groupQname) throws Exception {
		if (targetsOfGroup.containsKey(groupQname)) return targetsOfGroup.get(groupQname);
		synchronized (LOCK) {
			if (targetsOfGroup.containsKey(groupQname)) return targetsOfGroup.get(groupQname);
			List<EvaluationTarget> targets = new ArrayList<>();
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

	public YearlyGroupStat getStat(int year, String groupQname) {
		synchronized (LOCK) {
			if (!stats.containsKey(year)) {
				stats.put(year, new HashMap<String, YearlyGroupStat>());
			}
			if (!stats.get(year).containsKey(groupQname)) {
				stats.get(year).put(groupQname, new YearlyGroupStat(year, groupQname, this));
			}
			return stats.get(year).get(groupQname);
		}
	}

	public void setEvaluation(Evaluation evaluation) throws Exception {
		synchronized (LOCK) {
			String speciesQname = evaluation.getSpeciesQname();
			EvaluationTarget target = getTarget(speciesQname);
			setEvaluation(evaluation, target);
		}
	}

	private void setEvaluation(Evaluation evaluation, EvaluationTarget target) {
		target.setEvaluation(evaluation);
		invalidateStats(evaluation, target);
	}

	private void invalidateStats(Evaluation evaluation, EvaluationTarget target) {
		for (String groupQname : target.getGroups()) {
			getStat(evaluation.getEvaluationYear(), groupQname).invalidate();
		}
	}

	public void moveEvaluation(Evaluation evaluation, EvaluationTarget from, EvaluationTarget to) {
		synchronized (LOCK) {
			from.removeEvaluation(evaluation);
			setEvaluation(evaluation, to);
			invalidateStats(evaluation, from);
			invalidateStats(evaluation, to);
		}
	}

	public void deleteEvaluation(Evaluation evaluation, EvaluationTarget from) {
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
			for (EvaluationTarget target : getTargetsOfGroup(groupQname)) {
				for (Evaluation evaluation : target.getEvaluations()) {
					if (evaluation.hasRemarks()) {
						addRemarks(generateRemarks(evaluation, target), remarks);
					}
				}
			}
			remarksOfGroup.put(groupQname, remarks);
			return remarks;
		}
	}

	private Collection<Remark> generateRemarks(Evaluation evaluation, EvaluationTarget target) {
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

	public void removeRemark(EvaluationTarget target, int deletedId) throws Exception {
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

	public void addRemark(EvaluationTarget target, Evaluation evaluation) throws Exception {
		for (String groupQname : target.getGroups()) {
			addRemarks(generateRemarks(evaluation, target), getRemarksForGroup(groupQname));
		}
	}

	public void complateLoading(Evaluation evaluation) throws Exception {
		if (evaluation.isIncompletelyLoaded()) {
			synchronized (LOCK) {
				if (evaluation.isIncompletelyLoaded()) {
					iucnDAO.completeLoading(evaluation);
				}
			}
		}
	}

}
