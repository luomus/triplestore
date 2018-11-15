package fi.luomus.triplestore.taxonomy.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Content.Context;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.User;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/taxon-descriptions/*"})
public class TaxonDescriptionsServlet extends TaxonomyEditorBaseServlet {

	public static final Qname SPECIES_DESC_VARIABLES = new Qname("MX.speciesDescriptionVariables");
	private static final long serialVersionUID = -2281661002076649983L;

	private static final SingleObjectCacheResourceInjected<List<RdfProperty>, TriplestoreDAO> cachedDescriptionGroups = 
			new SingleObjectCacheResourceInjected<List<RdfProperty>, TriplestoreDAO>(
					new SingleObjectCacheResourceInjected.CacheLoader<List<RdfProperty>, TriplestoreDAO>() {
						@Override
						public List<RdfProperty> load(TriplestoreDAO dao) {
							try {
								return dao.getAltValues(SPECIES_DESC_VARIABLES);
							} catch (Exception e) {
								throw new RuntimeException("Loading desc groups", e);
							}
						}
					}, 10);
	
	public static final SingleObjectCacheResourceInjected<Map<String, List<RdfProperty>>, TriplestoreDAO> cachedDescriptionGroupVariables = 
			new SingleObjectCacheResourceInjected<Map<String, List<RdfProperty>>, TriplestoreDAO>(
					new SingleObjectCacheResourceInjected.CacheLoader<Map<String, List<RdfProperty>>, TriplestoreDAO>() {
						@Override
						public Map<String, List<RdfProperty>> load(TriplestoreDAO dao) {
							try {
								Map<String, List<RdfProperty>> descriptionGroupVariables = new LinkedHashMap<>();
								List<RdfProperty> descriptionGroups = cachedDescriptionGroups.get(dao);
								for (RdfProperty descriptionGroup : descriptionGroups) {
									descriptionGroupVariables.put(descriptionGroup.getQname().toString(), dao.getAltValues(descriptionGroup.getQname()));
								}
								return descriptionGroupVariables;
							} catch (Exception e) {
								throw new RuntimeException("Loading desc variables", e);
							}
						}
					}, 10);

	public static final Set<User.Role> ALLOWED = Collections.unmodifiableSet(Utils.set(User.Role.ADMIN, User.Role.NORMAL_USER, User.Role.DESCRIPTION_WRITER));
	
	@Override
	protected Set<User.Role> allowedRoles() {
		return ALLOWED;
	}
	
	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		Qname taxonQname = new Qname(getQname(req));
		if (!taxonQname.isSet()) {
			taxonQname = TaxonomyTreesEditorServlet.DEFAULT_ROOT_QNAME;
		}
		
		TriplestoreDAO dao = getTriplestoreDAO();
		TaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		if (!taxonomyDAO.getTaxonContainer().hasTaxon(taxonQname)) {
			return status404(res);
		}
		
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(taxonQname);

		Map<String, List<RdfProperty>> descriptionGroupVariables = cachedDescriptionGroupVariables.get(dao);
		Set<String> groupsWithContent = resolveGroupsWithContent(descriptionGroupVariables, taxon);

		if (taxon.getChecklist() != null) {
			responseData.setData("checklist", taxonomyDAO.getChecklists().get(taxon.getChecklist().toString()));
		}

		return responseData
				.setViewName("taxonDescriptions")
				.setData("taxon", taxon)
				.setData("root", taxon)
				.setData("variables", descriptionGroupVariables)
				.setData("groups", cachedDescriptionGroups.get(dao))
				.setData("groupsWithContent", groupsWithContent);
	}

	private Set<String> resolveGroupsWithContent(Map<String, List<RdfProperty>> descriptionGroupVariables, Taxon taxon) {
		Set<String> groupsWithContent = new HashSet<>();
		for (Map.Entry<String, List<RdfProperty>> e : descriptionGroupVariables.entrySet()) {
			String group = e.getKey();
			List<RdfProperty> descriptionVariables = e.getValue(); 
			if (hasContent(taxon, descriptionVariables)) {
				groupsWithContent.add(group);
			}
		}
		return groupsWithContent;
	}

	private boolean hasContent(Taxon taxon, List<RdfProperty> descriptionVariables) {
		Context defaultContext = taxon.getDescriptions().getDefaultContext();
		if (defaultContext == null) return false;

		for (RdfProperty descriptionVariable : descriptionVariables) {
			String property = descriptionVariable.getQname().toString();
			if (given(defaultContext.getText(property, "fi"))) {
				return true;
			} else if (given(defaultContext.getText(property, "sv"))) {
				return true;
			} else if (given(defaultContext.getText(property, "en"))) {
				return true;
			}
		}
		return false;
	}

}
