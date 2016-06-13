package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAOImple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IUCNContainer {

	private final TriplestoreDAO triplestoreDAO;
	private final IucnDAOImple iucnDAO;
	private final Map<Integer, Map<String, IUCNYearlyGroupStat>> stats = new HashMap<>();
	private final Map<String, List<IUCNEvaluationTarget>> targetsOfGroup = new HashMap<>();
	private final Map<String, List<String>> groupsOfTarget = new HashMap<>();
	private final Map<String, IUCNEvaluationTarget> targets = new HashMap<>();
	private static final Object LOCK = new Object();

	public IUCNContainer(TriplestoreDAO triplestoreDAO, IucnDAOImple iucnDAO) {
		this.triplestoreDAO = triplestoreDAO;
		this.iucnDAO = iucnDAO;
	}

	public IUCNEvaluationTarget getTarget(String speciesQname) throws Exception {
		if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
		synchronized (LOCK) {
			if (targets.containsKey(speciesQname)) return targets.get(speciesQname);
			IUCNEvaluationTarget target = iucnDAO.loadTarget(speciesQname);
			targets.put(speciesQname, target);
			return target;
		}
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
			target.setEvaluation(evaluation);
			for (String groupQname : target.getGroups()) {
				getStat(evaluation.getYear(), groupQname).invalidate();
			}
		}
	}

	public IUCNEvaluation markNotEvaluated(String speciesQname, int year, Qname editorQname) throws Exception {
		synchronized (LOCK) {
			Qname evaluationId = triplestoreDAO.getSeqNextValAndAddResource(IUCNEvaluation.IUCN_EVALUATION_NAMESPACE);
			IUCNEvaluation evaluation = IUCNEvaluation.notEvaluated(evaluationId, speciesQname, year, editorQname);
			triplestoreDAO.store(evaluation.getModel());
			this.setEvaluation(evaluation);
			return evaluation;	
		}
	}

}
