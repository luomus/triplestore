package fi.luomus.triplestore.service;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.languagesupport.LanguageFileReader;

public class TestConfig {

	public static Config getTriplestoreConfig() {
		return getConfig("triplestore-v2.properties");
	}

	public static Config getTaxonEditorConfig() {
		return getConfig("triplestore-v2-taxonomyeditor.properties");
	}

	private static Config getConfig(String cfgFile) {
		try {
			String base = System.getenv("CATALINA_HOME");
			if (base == null) base = "C:/apache-tomcat";
			File file = new File(base);
			if (!file.exists()) file = new File(System.getProperty("user.home"));
			String fullPath = file.getAbsolutePath() + File.separator + "app-conf" + File.separator + cfgFile;
			System.out.println("Using test config " + fullPath);
			Config config = new ConfigReader(fullPath);
			return config;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void all_locales_exist() throws Exception {
		new LanguageFileReader(getTriplestoreConfig()).readUITexts();
	}

	@Test
	public void all_locales_exist_taxoneditor() throws Exception {
		new LanguageFileReader(getTriplestoreConfig()).readUITexts();
	}
}
