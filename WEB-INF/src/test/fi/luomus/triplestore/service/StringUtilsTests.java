package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fi.luomus.triplestore.utils.StringUtils;

public class StringUtilsTests {

	@Test
	public void longText() {
		String längText = "läng text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng. Lång text is läng.";
		assertEquals(4255, längText.length());
		assertEquals(4703, StringUtils.countOfUTF8Bytes(längText));
		String result = StringUtils.sanitizeLiteral(längText);
		assertEquals(3614, result.length());
		assertEquals(3995, StringUtils.countOfUTF8Bytes(result));
	}

	@Test
	public void styles() { // Allow but validator warns
		String content = "<p style=\"color: red;\">Jee</p>";
		assertEquals(content, StringUtils.sanitizeLiteral(content));
	}

	@Test
	public void tags() {
		assertEquals("Foo bar", StringUtils.sanitizeLiteral("Foo <iframe href=\"...\"></iframe> bar"));
		assertEquals("Foo", StringUtils.sanitizeLiteral("Foo <iframe src=\"http://...\"></iframe>"));
		assertEquals("Foo <p>bar</p>", StringUtils.sanitizeLiteral("Foo <p>bar"));
		assertEquals("Foo < p >bar", StringUtils.sanitizeLiteral("Foo < p >bar"));
		assertEquals("Foo bar", StringUtils.sanitizeLiteral("Foo bar</p>"));
		assertEquals("Foo bar< /p>", StringUtils.sanitizeLiteral("Foo bar< /p>"));
		assertEquals("Foo <a href=\"http://..\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\">bar</a>"));
		assertEquals("Foo <a href=\"http://..\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\">bar"));
		assertEquals("Foo <a href=\"http://..\"></a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\"bar</a>"));
		assertEquals("Foo", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\"bar"));
		assertEquals("Foo <a href=\"/relative/MX.1\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"/relative/MX.1\">bar</a>"));
	}

	@Test
	public void escapedTags() {
		assertEquals("<script>alert('Hello');</script>", StringUtils.sanitizeLiteral("&lt;script&gt;alert('Hello');&lt;/script&gt;")); // We have to allow this
	}

	@Test
	public void noEntities() {
		assertEquals("\"Hornet's nest\"", StringUtils.sanitizeLiteral("\"Hornet's nest\""));
		assertEquals("<p>Letter ó</p>", StringUtils.sanitizeLiteral("<p>Letter ó</p>"));
		assertEquals("\"Linnea\"", StringUtils.sanitizeLiteral("\"Linnea\""));
		assertEquals("<p><5</p>", StringUtils.sanitizeLiteral("<p><5</p>"));
		assertEquals("Linnea & Goris", StringUtils.sanitizeLiteral("Linnea & Goris"));
	}

	@Test
	public void shortName() {
		assertEquals(null, StringUtils.shortName(null));
		assertEquals("", StringUtils.shortName(""));
		assertEquals("sortOrder", StringUtils.shortName("sortOrder"));
		assertEquals("dataset", StringUtils.shortName("GX.dataset"));
		assertEquals("dataset", StringUtils.shortName("GX.dataset.subitem"));
		assertEquals("dataset", StringUtils.shortName("GX.dataset.subitem:sub"));
		assertEquals("bibliographicCitation", StringUtils.shortName("dc:bibliographicCitation"));
	}

}
