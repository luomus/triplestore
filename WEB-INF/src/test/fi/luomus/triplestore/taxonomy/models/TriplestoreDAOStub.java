package fi.luomus.triplestore.taxonomy.models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.Checklist;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.taxonomy.Occurrences;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.triplestore.dao.RDFValidationException;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreSearchDAO;
import fi.luomus.triplestore.models.ResourceListing;
import fi.luomus.triplestore.models.UsedAndGivenStatements;

public class TriplestoreDAOStub implements TriplestoreDAO {

	@Override
	public Qname getSeqNextValAndAddResource(String qnamePrefix) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Checklist store(Checklist checklist) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public InformalTaxonGroup storeInformalTaxonGroup(InformalTaxonGroup group) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Taxon addTaxon(EditableTaxon taxon) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void store(Subject subject, Statement statement) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void insert(Subject subject, Statement statement) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public RdfProperties getProperties(String className) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public RdfProperty getProperty(Predicate predicate) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public List<RdfProperty> getAltValues(Qname qname) throws Exception {
		return null;
	}

	@Override
	public Model get(String qname) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Model get(Qname qname) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void store(Model model) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void delete(Subject subject) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void deleteStatement(long id) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void delete(Subject subject, Predicate predicate) throws SQLException {
		// Auto-generated method stub

	}

	@Override
	public void delete(Subject subject, Predicate predicate, Context context) throws SQLException {
		// Auto-generated method stub

	}

	@Override
	public void store(Subject subject, UsedAndGivenStatements usedAndGivenStatements) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public Publication storePublication(Publication publication) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void clearCaches() {
		// Auto-generated method stub

	}

	@Override
	public void store(Qname taxonQname, Occurrence occurrence) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public TriplestoreSearchDAO getSearchDAO() {
		// Auto-generated method stub
		return null;
	}

	@Override
	public long getUserFK(String userQname) throws Exception {
		// Auto-generated method stub
		return 0;
	}

	@Override
	public TransactionConnection openConnection() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Qname addResource(Qname qname) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public List<ResourceListing> getResourceStats() throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public boolean resourceExists(String qname) throws Exception {
		// Auto-generated method stub
		return false;
	}

	@Override
	public boolean resourceExists(Qname resourceQname) throws Exception {
		// Auto-generated method stub
		return false;
	}

	@Override
	public void store(Evaluation givenData, Evaluation existingEvaluation) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void store(Occurrences existingOccurrences, Occurrences newOccurrences, Set<Qname> supportedAreas) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public void store(HabitatObject habitat) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public RedListEvaluationGroup storeIucnRedListTaxonGroup(RedListEvaluationGroup group) throws Exception {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void storeOnlyOccurrences(Evaluation givenData, Evaluation existingEvaluation) throws Exception {
		// Auto-generated method stub

	}

	@Override
	public RuntimeException exception(String message, Exception e) {
		e.printStackTrace();
		return new RuntimeException(message, e);
	}

	@Override
	public Map<String, List<RdfProperty>> getDescriptionGroupVariables() {
		List<RdfProperty> l = new ArrayList<>();
		RdfProperty p = new RdfProperty(new Qname("MX.descriptionText"), new Qname("xsd:string"));
		p.setLabels(new LocalizedText().set("fi", "Yleiskuvaus").set("en", "General description"));
		l.add(p);

		Map<String, List<RdfProperty>> map = new HashMap<>();
		map.put("MX.descGroup", l);
		return map;
	}

	@Override
	public List<RdfProperty> getDescriptionGroups() {
		return null;

	}

	@Override
	public int removeUnusedPublications() {
		// Auto-generated method stub
		return -1;
	}

	@Override
	public void validate(Model model) throws RDFValidationException {
		// Auto-generated method stub

	}

}
