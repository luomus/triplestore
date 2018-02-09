package fi.luomus.triplestore.utils;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Whitelist;

import fi.luomus.commons.utils.Utils;

public class StringUtils {

	private static final String SHORTNAME_SEPARATOR_REGEX = Pattern.quote(".")+"|"+Pattern.quote(":");
	public static final String ALLOWED_TAGS = "p, a, b, strong, i, em, ul, li";
	private static final Whitelist WHITELIST;
	private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false).escapeMode(EscapeMode.xhtml);

	static {
		WHITELIST = Whitelist.none()
				.addAttributes("p", "style")
				.addAttributes("a", "href");
		for (String tag : ALLOWED_TAGS.split(Pattern.quote(","))) {
			tag = tag.trim();
			WHITELIST.addTags(tag);
		}
	}

	public static int countOfUTF8Bytes(String s) {
		return toUTF8Bytes(s).length;
	}

	public static String sanitizeLiteral(String content) {
		if (!given(content)) return "";
		content = Utils.removeWhitespaceAround(content);
		if (!given(content)) return "";
		if (content.length() >= 1000) {
			content = StringUtils.trimToByteLength(content, 4000);
		}
		content = Jsoup.clean(content, "", WHITELIST, OUTPUT_SETTINGS);
		content = content.replace("<p></p>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim();
		while (content.contains("  ")) {
			content = content.replace("  ", " ");
		}
		if (content.endsWith("\n")) {
			content = content.substring(0, content.length()-1);
		}
		return content.trim();
	}

	private static boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}

	private static String trimToByteLength(String s, int size) {
		if (s == null) return null;
		byte[] utf8 = toUTF8Bytes(s);
		while (utf8.length > size) {
			s = s.substring(0, s.length() - 10);
			utf8 = toUTF8Bytes(s);
		}
		return s;
	}

	private static byte[] toUTF8Bytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String shortName(String qname) {
		if (qname == null) return null;
		if (!qname.contains(".")) return qname;
		String[] parts = qname.split(Pattern.quote("."));
		if (parts.length == 2) {
			return parts[1];
		}
		String shortName = "";
		for (int i = 1; i<parts.length; i++) {
			shortName += parts[i];
			if (i < parts.length-1) shortName += ".";
		}
		return shortName;
	}

}
