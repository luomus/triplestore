package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CriteriaFormatValidator {

	private static final Map<String, CriteriaFormatValidator> VALIDATORS = new LinkedHashMap<>(); 
	static {
		CriteriaFormatValidator a = new CriteriaFormatValidator();
		a.addMainCriteria("A1").addSubCriterias("a", "b", "c", "d", "e");
		a.addMainCriteria("A2").addSubCriterias("a", "b", "c", "d", "e");
		a.addMainCriteria("A3").addSubCriterias("b", "c", "d", "e");
		a.addMainCriteria("A4").addSubCriterias("a", "b", "c", "d", "e");
		
		CriteriaFormatValidator b = new CriteriaFormatValidator();
		b.addMainCriteria("B1").addSubCriterias("a", "b", "c");
		b.getMainCriteria("B1").getSubCriteria("b").addSpecifications("i", "ii", "iii", "iv", "v");
		// TODO
		CriteriaFormatValidator c = new CriteriaFormatValidator();
		
		CriteriaFormatValidator d = new CriteriaFormatValidator();
		
		CriteriaFormatValidator e = new CriteriaFormatValidator();
		e.addMainCriteria("E");
		
		VALIDATORS.put("A", a);
		VALIDATORS.put("B", b);
		VALIDATORS.put("C", c);
		VALIDATORS.put("D", d);
		VALIDATORS.put("E", e);
	}
	
	private static class MainCriteria {

		private final Map<String, SubCriteria> subCriterias = new LinkedHashMap<>();
		
		public MainCriteria(String mainCriteria) {
			// TODO Auto-generated constructor stub
		}

		public SubCriteria getSubCriteria(String subCriteria) {
			return subCriterias.get(subCriteria);
		}

		public MainCriteria addSubCriterias(String ... subCriteria) {
			for (String subCr : subCriteria) {
				subCriterias.put(subCr, new SubCriteria(subCr));	
			}
			return this;
		}
	}

	private static class SubCriteria {

		private final Set<String> specifications = new LinkedHashSet<>();
		
		public SubCriteria(String subCriteria) {
			// TODO Auto-generated constructor stub
		}

		public void addSpecifications(String ... specifications) {
			for (String s : specifications) {
				this.specifications.add(s);
			}
		}
		
	}
	
	private final Map<String, MainCriteria> mainCriterias = new LinkedHashMap<>(); 
		
	public static CriteriaFormatValidator forCriteria(String criteria) {
		if (!VALIDATORS.containsKey(criteria)) throw new IllegalArgumentException("Unknown criteria " + criteria);
		return VALIDATORS.get(criteria);
	}

	private MainCriteria getMainCriteria(String mainCriteria) {
		return mainCriterias.get(mainCriteria);
	}

	private MainCriteria addMainCriteria(String mainCriteria) {
		MainCriteria m = new MainCriteria(mainCriteria);
		this.mainCriterias.put(mainCriteria, m);
		return m;
	}

	public boolean validate(String value) {
		// TODO Auto-generated method stub
		return false;
	}

	public static boolean validateJoined(String string) {
		// TODO Auto-generated method stub
		return false;
	}

}
