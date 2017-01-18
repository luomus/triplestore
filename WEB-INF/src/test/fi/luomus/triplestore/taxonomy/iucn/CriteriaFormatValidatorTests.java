package fi.luomus.triplestore.taxonomy.iucn;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.taxonomy.iucn.model.CriteriaFormatValidator;
import fi.luomus.triplestore.taxonomy.iucn.model.CriteriaFormatValidator.MainCriteria;

public class CriteriaFormatValidatorTests {

	@Test
	public void number_and_isupper() {
		assertEquals(false, Character.isUpperCase('2'));
		assertEquals(true, Character.isDigit('2'));
		assertEquals(false, Character.isDigit('a'));
		assertEquals(false, Character.isDigit('A'));
	}

	@Test
	public void parsing_criterias() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("E");
		assertEquals(1, mainCriterias.size());
		assertEquals("E", mainCriterias.get(0).getMainCriteria());
		assertEquals(false, mainCriterias.get(0).hasSubCriterias());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().size());
	}

	@Test
	public void parsing_criterias_2() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A");
		assertEquals(1, mainCriterias.size());
		assertEquals("A", mainCriterias.get(0).getMainCriteria());
		assertEquals(false, mainCriterias.get(0).hasSubCriterias());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().size());
	}

	@Test
	public void parsing_criterias_3() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A2a");
		assertEquals(1, mainCriterias.size());
		assertEquals("A2", mainCriterias.get(0).getMainCriteria());
		assertEquals(true, mainCriterias.get(0).hasSubCriterias());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().size());
		assertEquals('a', mainCriterias.get(0).getSubCriterias().get(0).getSubCriteria());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().get(0).getSpecifications().size());
		assertEquals(false, mainCriterias.get(0).getSubCriterias().get(0).hasSpecifications());

	}

	@Test
	public void parsing_criterias_4() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A2a+3b");
		assertEquals(2, mainCriterias.size());
		assertEquals("A2", mainCriterias.get(0).getMainCriteria());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().size());
		assertEquals('a', mainCriterias.get(0).getSubCriterias().get(0).getSubCriteria());

		assertEquals("A3", mainCriterias.get(1).getMainCriteria());
		assertEquals(1, mainCriterias.get(1).getSubCriterias().size());
		assertEquals('b', mainCriterias.get(1).getSubCriterias().get(0).getSubCriteria());

	}

	@Test
	public void parsing_criterias_5() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("B1ab(iii)c(iv,ii)+2a(iii)c");
		assertEquals(2, mainCriterias.size());
		assertEquals("B1", mainCriterias.get(0).getMainCriteria());
		assertEquals(3, mainCriterias.get(0).getSubCriterias().size());

		assertEquals('a', mainCriterias.get(0).getSubCriterias().get(0).getSubCriteria());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().get(0).getSpecifications().size());

		assertEquals('b', mainCriterias.get(0).getSubCriterias().get(1).getSubCriteria());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().get(1).getSpecifications().size());
		assertEquals("iii", mainCriterias.get(0).getSubCriterias().get(1).getSpecifications().get(0));

		assertEquals('c', mainCriterias.get(0).getSubCriterias().get(2).getSubCriteria());
		assertEquals(2, mainCriterias.get(0).getSubCriterias().get(2).getSpecifications().size());
		assertEquals("iv", mainCriterias.get(0).getSubCriterias().get(2).getSpecifications().get(0));
		assertEquals("ii", mainCriterias.get(0).getSubCriterias().get(2).getSpecifications().get(1));
	}

	@Test
	public void parsing_criterias_6() {
		assertEquals(0, CriteriaFormatValidator.parseCriteria("asdasdasd").size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria("").size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria("  ").size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria(null).size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria(" a2A ").size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria("a2").size());
		assertEquals(0, CriteriaFormatValidator.parseCriteria("aA2").size());
		assertEquals(1, CriteriaFormatValidator.parseCriteria(" A ").size());
	}

	@Test
	public void parsing_criterias_7() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria(" E ");
		assertEquals(1, mainCriterias.size());
		assertEquals("E", mainCriterias.get(0).getMainCriteria());
		assertEquals(false, mainCriterias.get(0).hasSubCriterias());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().size());
	}

	@Test
	public void parsing_criterias_8() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("E;A");
		assertEquals(2, mainCriterias.size());
		assertEquals("E", mainCriterias.get(0).getMainCriteria());
		assertEquals(false, mainCriterias.get(0).hasSubCriterias());

		assertEquals("A", mainCriterias.get(1).getMainCriteria());
		assertEquals(false, mainCriterias.get(1).hasSubCriterias());
	}

	@Test
	public void parsing_criterias_9() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A+B");
		assertEquals(2, mainCriterias.size());
		assertEquals("A", mainCriterias.get(0).getMainCriteria());
		assertEquals(false, mainCriterias.get(0).hasSubCriterias());

		assertEquals("AB", mainCriterias.get(1).getMainCriteria());
		assertEquals(false, mainCriterias.get(1).hasSubCriterias());
	}

	@Test
	public void parsing_criterias_10() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A2a()");
		assertEquals(1, mainCriterias.size());
		assertEquals("A2", mainCriterias.get(0).getMainCriteria());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().size());

		assertEquals(0, mainCriterias.get(0).getSubCriterias().get(0).getSpecifications().size());
	}

	@Test
	public void parsing_criterias_11() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("A2a(i+i)"); // -> A2a(i   +   A2i) 
		assertEquals(2, mainCriterias.size());
		assertEquals("A2", mainCriterias.get(0).getMainCriteria());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().size());
		assertEquals(0, mainCriterias.get(0).getSubCriterias().get(0).getSpecifications().size());

		assertEquals("A", mainCriterias.get(1).getMainCriteria());
		assertEquals(1, mainCriterias.get(1).getSubCriterias().size());
		assertEquals('i', mainCriterias.get(1).getSubCriterias().get(0).getSubCriteria());
		assertEquals(0, mainCriterias.get(1).getSubCriterias().get(0).getSpecifications().size());
	}

	@Test
	public void parsing_criterias_12() {
		List<MainCriteria> mainCriterias = CriteriaFormatValidator.parseCriteria("B1ab(i))");
		assertEquals(1, mainCriterias.size());
		assertEquals("B1", mainCriterias.get(0).getMainCriteria());
		assertEquals(2, mainCriterias.get(0).getSubCriterias().size());
		
		assertEquals(0, mainCriterias.get(0).getSubCriterias().get(0).getSpecifications().size());
		assertEquals(1, mainCriterias.get(0).getSubCriterias().get(1).getSpecifications().size());
		assertEquals("i", mainCriterias.get(0).getSubCriterias().get(1).getSpecifications().get(0));
	}
	
	@Test
	public void to_criteria_string() {
		MainCriteria mc1 = new MainCriteria("A1").addSubCriterias('a', 'b');
		mc1.getSubCriteria('b').addSpecifications("i", "iv");

		MainCriteria mc2 = new MainCriteria("A2").addSubCriterias('c');
		mc2.getSubCriteria('c').addSpecifications("ii");

		MainCriteria mc3 = new MainCriteria("D1");

		String actual = CriteriaFormatValidator.toCriteriaString(Utils.list(mc1, mc2, mc3));
		String expected = "A1ab(i,iv)+2c(ii); D1";
		assertEquals(expected, actual);
	}

	@Test
	public void test_single() {
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate(null).isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate(" E ").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate(" E").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate("E ").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A3b + 2d").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A3b +2d").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A3b+ 2d").isValid());

		assertEquals(true, CriteriaFormatValidator.forCriteria("E").validate("E").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate("E1").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate("E1a").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A7").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("A").validate("A2a").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A22a").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("A").validate("A2ab").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2abx").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2aa").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2abb").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2aba").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2ba").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2ab3").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("E").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("A").validate("A2abc+3de").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2abc+A3de").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2abc+de").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("A").validate("A2a(i)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1a").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1b").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1b(i)").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1b(i,ii)").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1b(i,ii,v)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1bi").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1b(xi)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1b(i,i)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1b(ii,i)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1b(i-iii)").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1ab(iii)c(iv)").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B2ab(iii)c(iv)").isValid());
		assertEquals(true, CriteriaFormatValidator.forCriteria("B").validate("B1ab(iii)c(iv)+2ab(iii)c(iv)").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1ab(iii)c(iv)+B2ab(iii)c(iv)").isValid());

		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1ab()").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1ab(i").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1ab(i))").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1a(i+ii))").isValid());
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B1a+b)").isValid());
		
		assertEquals(false, CriteriaFormatValidator.forCriteria("B").validate("B2a+1a").isValid());
	}
	
	@Test
	public void test_joined() {
		assertEquals(true, CriteriaFormatValidator.validateJoined("A1abc+2e; E").isValid());
		assertEquals(true, CriteriaFormatValidator.validateJoined("A1a; E").isValid());
		assertEquals(false, CriteriaFormatValidator.validateJoined("E; A1a").isValid());
		assertEquals(true, CriteriaFormatValidator.validateJoined("A1a; B1a").isValid());
		assertEquals(false, CriteriaFormatValidator.validateJoined("B1a; A1a").isValid());

		assertEquals(false, CriteriaFormatValidator.validateJoined("A1a;B1a").isValid());
		assertEquals(false, CriteriaFormatValidator.validateJoined("A1a;B1a + 2a").isValid());
	}

	@Test
	public void test_errormessages() {
		assertEquals(null, CriteriaFormatValidator.forCriteria("E").validate("E").getErrorMessage());
		
		assertEquals("Kriteeri A1 tulisi ilmoittaa ennen kriteeriä E", 
				CriteriaFormatValidator.validateJoined("E; A1a").getErrorMessage());
		
		assertEquals("Kriteeri A1 tulisi ilmoittaa ennen kriteeriä B1", 
				CriteriaFormatValidator.validateJoined("B1a; A1a").getErrorMessage());
		
		assertEquals("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: A1a;B1a, pitäisi olla A1a; B1a", 
				CriteriaFormatValidator.validateJoined("A1a;B1a").getErrorMessage());
		
		assertEquals("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: A1a;B1a + 2a, pitäisi olla A1a; B1a+2a", 
				CriteriaFormatValidator.validateJoined("A1a;B1a + 2a").getErrorMessage());
		
		assertEquals("Ohjelmointivirhe: null criteria", CriteriaFormatValidator.forCriteria("E").validate(null).getErrorMessage());
		assertEquals("Ohjelmointivirhe: ei ole poistettu whitespacea", CriteriaFormatValidator.forCriteria("E").validate(" E ").getErrorMessage());
		assertEquals("Ohjelmointivirhe: ei ole poistettu whitespacea", CriteriaFormatValidator.forCriteria("A").validate("A3b + 2d").getErrorMessage());

		assertEquals("Tuntematon kriteeri E1", CriteriaFormatValidator.forCriteria("E").validate("E1a").getErrorMessage());

		assertEquals("Kriteerille A2 on annettu alakriteeri a useammin kuin kerran", CriteriaFormatValidator.forCriteria("A").validate("A2aa").getErrorMessage());
		assertEquals("Kriteerin A2 alakriteerin a täytyy olla ennen alakriteeriä b", CriteriaFormatValidator.forCriteria("A").validate("A2aba").getErrorMessage());

		assertEquals("Kriteerille A2 ei ole määritelty alakriteeriä 3", CriteriaFormatValidator.forCriteria("A").validate("A2ab3").getErrorMessage());
		assertEquals("Tuntematon kriteeri E", CriteriaFormatValidator.forCriteria("A").validate("E").getErrorMessage());

		assertEquals("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: A2abc+A3de, pitäisi olla A2abc+3de", 
				CriteriaFormatValidator.forCriteria("A").validate("A2abc+A3de").getErrorMessage());

		assertEquals("Kriteerille A2a ei ole määritelty lisämäärettä i", CriteriaFormatValidator.forCriteria("A").validate("A2a(i)").getErrorMessage());
		assertEquals("Kriteerille B1 täytyy antaa yksi alakriteereistä [a, b, c]", CriteriaFormatValidator.forCriteria("B").validate("B1").getErrorMessage());
		assertEquals("Kriteerille B1b täytyy antaa vähintään yksi lisämääreistä [i, ii, iii, iv, v]", CriteriaFormatValidator.forCriteria("B").validate("B1b").getErrorMessage());
		assertEquals("Kriteerille B1b täytyy antaa vähintään yksi lisämääreistä [i, ii, iii, iv, v]", CriteriaFormatValidator.forCriteria("B").validate("B1bi").getErrorMessage());
		assertEquals("Kriteerille B1b ei ole määritelty lisämäärettä xi", CriteriaFormatValidator.forCriteria("B").validate("B1b(xi)").getErrorMessage());
		assertEquals("Kriteerille B1b on annettu lisämääre i useammin kuin kerran", CriteriaFormatValidator.forCriteria("B").validate("B1b(i,i)").getErrorMessage());
		assertEquals("Kriteerin B1b lisämääreen i tulis olla ennen lisämäärettä ii", CriteriaFormatValidator.forCriteria("B").validate("B1b(ii,i)").getErrorMessage());
		assertEquals("Kriteerille B1b ei ole määritelty lisämäärettä i-iii", CriteriaFormatValidator.forCriteria("B").validate("B1b(i-iii)").getErrorMessage());
		assertEquals("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: B1ab(iii)c(iv)+B2ab(iii)c(iv), pitäisi olla B1ab(iii)c(iv)+2ab(iii)c(iv)", 
				CriteriaFormatValidator.forCriteria("B").validate("B1ab(iii)c(iv)+B2ab(iii)c(iv)").getErrorMessage());

		assertEquals("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: B1ab(i, pitäisi olla B1ab", 
				CriteriaFormatValidator.forCriteria("B").validate("B1ab(i").getErrorMessage());
		
		assertEquals("Kriteeri B1 tulisi ilmoittaa ennen kriteeriä B2", CriteriaFormatValidator.forCriteria("B").validate("B2a+1a").getErrorMessage());
	}

}


