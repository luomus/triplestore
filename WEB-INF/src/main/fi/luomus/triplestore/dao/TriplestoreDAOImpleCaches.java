package fi.luomus.triplestore.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.ResourceListing;

class TriplestoreDAOImpleCaches {

	private static final Set<String> PERSON_ROLE_PREDICATES = Utils.set("MA.role", "MA.roleKotka", "MA.organisation");
	private static final String MA_FULL_NAME = "MA.fullName";
	private static final String MA_PERSON = "MA.person";
	private static final Qname SPECIES_DESC_VARIABLES = new Qname("MX.speciesDescriptionVariables");
	
	private final TriplestoreDAOImple dao;

	TriplestoreDAOImpleCaches(TriplestoreDAOImple dao) {
		this.dao = dao;
	}

	final SingleObjectCache<List<RdfProperty>> persons = new SingleObjectCache<>(new SingleObjectCache.CacheLoader<List<RdfProperty>>() {
		@Override
		public List<RdfProperty> load() {
			try {
				List<RdfProperty> rangeValues = new ArrayList<>();
				Collection<Model> persons = dao.getSearchDAO().search(
						new SearchParams(Integer.MAX_VALUE, 0)
						.type(MA_PERSON)
						.predicates(PERSON_ROLE_PREDICATES)); 
				for (Model m : persons) {
					RdfProperty rangeValue = new RdfProperty(new Qname(m.getSubject().getQname()), null);
					String personName = m.getSubject().getQname();
					for (Statement s : m.getStatements(MA_FULL_NAME)) {
						personName = s.getObjectLiteral().getContent();
						break;
					}
					rangeValue.setLabels(new LocalizedText().set("fi", personName).set("en", personName).set("sv", personName).set(null, personName));
					rangeValues.add(rangeValue);
				}
				return rangeValues;
			} catch (Exception e) {
				throw dao.exception("Person cache", e);
			}
		}
	}, 15, TimeUnit.MINUTES);

	final SingleObjectCache<List<ResourceListing>> stats = new SingleObjectCache<>(new ResourceStatCacheLoader(), 15, TimeUnit.MINUTES);

	private class ResourceStatCacheLoader implements SingleObjectCache.CacheLoader<List<ResourceListing>> {
		@Override
		public List<ResourceListing> load() {
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				con = dao.openConnection();
				p = con.prepareStatement(" SELECT resourcename, resourcecount FROM " + TriplestoreDAOConst.SCHEMA + ".rdf_classview_materialized ORDER BY resourcecount DESC ");
				rs = p.executeQuery();
				List<ResourceListing> listing = new ArrayList<>();
				while (rs.next()) {
					listing.add(new ResourceListing(rs.getString(1), rs.getInt(2)));
				}
				return listing;
			} catch (Exception e) {
				e.printStackTrace();
				return Collections.emptyList();
			} finally {
				Utils.close(p, rs, con);
			}
		}
	}

	final Cached<String, RdfProperties> properties = new Cached<>(new PropertiesCacheLoader(), 1, TimeUnit.HOURS, 500);

	private final static String GET_PROPERTIES_BY_CLASSNAME_SQL = "" + 
			" SELECT DISTINCT propertyName 										" + 
			" FROM																" +
			" ((																" +
			" 	 SELECT DISTINCT v.predicatename AS propertyName				" +
			" 	 FROM "+TriplestoreDAOConst.SCHEMA+".rdf_statementview v 							" +
			"    WHERE v.subjectname IN ( 										" +																				
			" 	   SELECT DISTINCT subjectname FROM "+TriplestoreDAOConst.SCHEMA+".rdf_statementview WHERE predicatename = 'rdf:type' AND objectname = ?		" + 				
			" 	 ) 																" +
			" ) UNION (															" +
			"   SELECT DISTINCT subjectname as propertyName						" +
			"   FROM "+TriplestoreDAOConst.SCHEMA+".rdf_statementview 								" +
			"   WHERE predicatename = 'rdfs:domain' AND objectname = ?			" +
			" )) properties														";

	private class PropertiesCacheLoader implements CacheLoader<String, RdfProperties> {

		@Override
		public RdfProperties load(String className) {
			TransactionConnection con = null;
			PreparedStatement p = null;
			ResultSet rs = null;
			try {
				con = dao.openConnection();
				p = con.prepareStatement(GET_PROPERTIES_BY_CLASSNAME_SQL);
				p.setString(1, className);
				p.setString(2, className);
				rs = p.executeQuery();
				RdfProperties properties = new RdfProperties();
				Set<Qname> propertyQnames = new HashSet<>();
				while (rs.next()) {
					propertyQnames.add(new Qname(rs.getString(1)));
				}
				for (Model model : dao.getSearchDAO().get(propertyQnames)) {
					RdfProperty property = dao.createProperty(model);
					properties.addProperty(property);
				}
				return properties;
			} catch (Exception e) {
				throw dao.exception("Properties cache loader for classname " + className + ". " + e.getMessage(), e);
			}
			finally {
				Utils.close(p, rs, con);
			}
		}
	}

	final Cached<String, RdfProperty> propery = new Cached<>(new SinglePropertyCacheLoader(), 1, TimeUnit.HOURS, 10000);

	private class SinglePropertyCacheLoader implements CacheLoader<String, RdfProperty> {

		@Override
		public RdfProperty load(String predicateQname) {
			try {
				Model model = dao.get(predicateQname);
				RdfProperty property = dao.createProperty(model);
				return property;
			} catch (Exception e) {
				throw new RuntimeException("Single property cache loader for predicate " + predicateQname + ". " + e.getMessage());
			}
		}

	}

	final SingleObjectCache<List<RdfProperty>> descriptionGroups = 
			new SingleObjectCache<>(
					new SingleObjectCache.CacheLoader<List<RdfProperty>>() {
						@Override
						public List<RdfProperty> load() {
							try {
								return dao.getAltValues(SPECIES_DESC_VARIABLES);
							} catch (Exception e) {
								throw dao.exception("Loading desc groups", e);
							}
						}
					}, 1, TimeUnit.HOURS);

	final SingleObjectCache<Map<String, List<RdfProperty>>> descriptionGroupVariables = 
			new SingleObjectCache<>(
					new SingleObjectCache.CacheLoader<Map<String, List<RdfProperty>>>() {
						@Override
						public Map<String, List<RdfProperty>> load() {
							try {
								Map<String, List<RdfProperty>> descriptionGroupVariables = new LinkedHashMap<>();
								for (RdfProperty descriptionGroup : descriptionGroups.get()) {
									descriptionGroupVariables.put(descriptionGroup.getQname().toString(), dao.getAltValues(descriptionGroup.getQname()));
								}
								return descriptionGroupVariables;
							} catch (Exception e) {
								throw dao.exception("Loading desc variables", e);
							}
						}
					}, 1, TimeUnit.HOURS);
	
	public void invalidateAll() {
		properties.invalidateAll();
		propery.invalidateAll();
		persons.invalidate();
		stats.invalidate();
		descriptionGroups.invalidate();
		descriptionGroupVariables.invalidate();
	}

}
