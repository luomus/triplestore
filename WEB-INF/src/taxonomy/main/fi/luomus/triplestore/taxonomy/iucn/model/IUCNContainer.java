package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAOImple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IUCNContainer {

	private static final Object LOCK = new Object();

	private final TriplestoreDAO triplestoreDAO;
	private final IucnDAOImple iucnDAO;
	private final Map<Integer, Map<String, IUCNYearlyGroupStat>> stats = new HashMap<>();
	private final Map<String, List<IUCNEvaluationTarget>> targetsOfGroup = new HashMap<>();
	private final Map<String, List<String>> groupsOfTarget = new HashMap<>();
	private final Map<String, IUCNEvaluationTarget> targets = new HashMap<>();

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
				getStat(evaluation.getEvaluationYear(), groupQname).invalidate();
			}
		}
	}

	public IUCNEvaluation markNotEvaluated(String speciesQname, int year, Qname editorQname) throws Exception {
		synchronized (LOCK) {
			IUCNEvaluation evaluation = createNotEvaluatedEvaluation(speciesQname, year, editorQname);
			triplestoreDAO.store(evaluation.getModel());
			this.setEvaluation(evaluation);
			return evaluation;	
		}
	}

	private IUCNEvaluation createNotEvaluatedEvaluation(String speciesQname, int year, Qname editorQname) throws Exception {
		IUCNEvaluation evaluation = iucnDAO.createEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATED_TAXON), new ObjectResource(speciesQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.RED_LIST_STATUS), new ObjectResource("MX.iucnNE")));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.RED_LIST_STATUS_NOTES), new ObjectLiteral(IUCNEvaluation.NE_MARK_NOTES, "fi")));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.STATE), new ObjectResource(IUCNEvaluation.STATE_READY)));
		return evaluation;
	}

}
