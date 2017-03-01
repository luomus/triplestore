package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;

import fi.luomus.triplestore.utils.StringUtils;

import org.junit.Test;

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
		assertEquals("Foo <p>bar</p>", StringUtils.sanitizeLiteral("Foo <p>bar"));
		assertEquals("Foo &lt; p &gt;bar", StringUtils.sanitizeLiteral("Foo < p >bar"));
		assertEquals("Foo bar", StringUtils.sanitizeLiteral("Foo bar</p>"));
		assertEquals("Foo bar&lt; /p&gt;", StringUtils.sanitizeLiteral("Foo bar< /p>"));
		assertEquals("Foo <a href=\"http://..\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\">bar</a>"));
		assertEquals("Foo <a href=\"http://..\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\">bar"));
		assertEquals("Foo <a href=\"http://..\"></a>", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\"bar</a>"));
		assertEquals("Foo", StringUtils.sanitizeLiteral("Foo <a href=\"http://..\"bar"));
		assertEquals("Foo <a href=\"/relative/MX.1\">bar</a>", StringUtils.sanitizeLiteral("Foo <a href=\"/relative/MX.1\">bar</a>"));
		
		assertEquals("Foo bar", StringUtils.sanitizeLiteral("Foo <iframe href=\"...\"></iframe> bar"));
	}
	
}
