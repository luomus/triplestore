package fi.luomus.triplestore.taxonomy.dao;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.Filter;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.TripletToTaxonHandler;
import fi.luomus.commons.taxonomy.TripletToTaxonHandlers;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class CachedLiveLoadingTaxonContainer implements TaxonContainer {

	private static final String MX_IS_PART_OF = "MX.isPartOf";

	private static final Object LOCK = new Object();

	private final TripletToTaxonHandlers tripletToTaxonHandlers = new TripletToTaxonHandlers();
	private final Cached<Qname, EditableTaxon> cachedTaxons = new Cached<Qname, EditableTaxon>(new TaxonLoader(), 3*60*60, 25000);
	private final Cached<Qname, Set<Qname>> cachedChildren = new Cached<Qname, Set<Qname>>(new ChildrenLoader(), 3*60*60, 25000);
	private final TriplestoreDAO triplestoreDAO;


	public CachedLiveLoadingTaxonContainer(TriplestoreDAO triplestoreDAO) {
		this.triplestoreDAO = triplestoreDAO;
	}

	private class TaxonLoader implements CacheLoader<Qname, EditableTaxon> {
		@Override
		public EditableTaxon load(Qname qname) {
			try {
				Model model = triplestoreDAO.get(qname);
				if (model.isEmpty()) throw new NoSuchTaxonException(qname);
				return createTaxon(model);
			} catch (NoSuchTaxonException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public EditableTaxon createTaxon(Model model) {
		Qname qname = q(model.getSubject());
		EditableTaxon taxon = new EditableTaxon(qname, this);
		for (Statement statement : model.getStatements()) {
			String predicate = statement.getPredicate().getQname();
			Qname object = q(statement);
			if (MX_IS_PART_OF.equals(predicate)) {
				taxon.setParentQname(object);
			} else {
				addPropertyToTaxon(taxon, statement);
			}
		}
		return taxon;
	}

	private Qname q(Statement statement) {
		if (statement.isResourceStatement()) {
			return q(statement.getObjectResource());
		}
		return null;
	}

	private Qname q(RdfResource resource) {
		return new Qname(resource.getQname());
	}

	private void addPropertyToTaxon(Taxon taxon, Statement statement) {
		Qname context = statement.isForDefaultContext() ? null : q(statement.getContext());
		Qname predicatename = q(statement.getPredicate());
		Qname objectname = null;
		String resourceliteral = null;
		String langcode = null;

		if (statement.isLiteralStatement()) {
			resourceliteral = statement.getObjectLiteral().getContent();
			langcode = statement.getObjectLiteral().getLangcode();
			if (!given(langcode)) langcode = null;
		} else {
			objectname = q(statement.getObjectResource());
		}

		TripletToTaxonHandler handler = tripletToTaxonHandlers.getHandler(predicatename);
		handler.setToTaxon(context, predicatename, objectname, resourceliteral, langcode, taxon);
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	private class ChildrenLoader implements CacheLoader<Qname, Set<Qname>> {
		@Override
		public Set<Qname> load(Qname taxonQname) {
			try {
				Set<Qname> childTaxons = new HashSet<Qname>();
				Collection<Model> models = triplestoreDAO.getSearchDAO().search(MX_IS_PART_OF, taxonQname.toString());
				for (Model model : models) {
					EditableTaxon child = createTaxon(model);
					cachedTaxons.put(child.getQname(), child);
					childTaxons.add(child.getQname());
				}
				return childTaxons;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public EditableTaxon getTaxon(Qname taxonId) throws NoSuchTaxonException {
		return cachedTaxons.get(taxonId);
	}

	@Override
	public boolean hasTaxon(Qname taxonId) {
		try {
			cachedTaxons.get(taxonId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Set<Qname> getChildren(Qname parentId) {
		return cachedChildren.get(parentId);
	}

	public void clearCaches() {
		cachedChildren.invalidateAll();
		cachedTaxons.invalidateAll();
	}

	public void invalidateTaxon(Taxon taxon) {
		synchronized (LOCK) {
			alreadyInvalidatedTaxonsInIsolation.clear();
			invalidateTaxonInIsolation(taxon);
			alreadyInvalidatedTaxonsInIsolation.clear();
		}

	}

	private final Set<Qname> alreadyInvalidatedTaxonsInIsolation = new HashSet<>();

	private void invalidateTaxonInIsolation(Taxon taxon) {
		if (alreadyInvalidatedTaxonsInIsolation.contains(taxon.getQname())) return;
		alreadyInvalidatedTaxonsInIsolation.add(taxon.getQname());
		for (Taxon synonym : taxon.getAllSynonyms()) {
			invalidateTaxonInIsolation(synonym);
		}
		if (taxon.hasParent()) {
			invalidateTaxonInIsolation(taxon.getParent());
		}
		cachedTaxons.invalidate(taxon.getQname());
		cachedChildren.invalidate(taxon.getQname());
	}

	@Override
	public int getNumberOfTaxons() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getInformalGroupFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getAdminStatusFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getRedListStatusFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter getTypesOfOccurrenceFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getInvasiveSpeciesFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getHasDescriptionsFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getHasMediaFilter() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Qname> getInvasiveSpeciesEarlyWarningFilter() {
		throw new UnsupportedOperationException();
	}

}

