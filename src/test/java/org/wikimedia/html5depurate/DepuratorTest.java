package org.wikimedia.html5depurate;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import org.xml.sax.InputSource;

import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DepuratorTest {
	private String input;
	private String expected;
	private boolean compat;

	@Parameters
	public static Collection<Object[]> data() {
		final boolean COMPAT = true;
		final boolean NOCOMPAT = false;

		return Arrays.asList(new Object[][] {
			// Tests are indexed for cross-referencing in junit output
			// 0. Empty string
			{COMPAT, "", ""},
			// 1. Simple p-wrap
			{COMPAT, "x", "<p>x</p>"},
			// 2. No p-wrap of blank node
			{COMPAT, " ", " "},
			// 3. p-wrap terminated by div
			{COMPAT, "x<div></div>", "<p>x</p><div></div>"},
			// 4. p-wrap not terminated by span
			{COMPAT, "x<span></span>", "<p>x<span></span></p>"},
			// 5. An element is non-blank and so gets p-wrapped
			{COMPAT, "<span></span>", "<p><span></span></p>"},
			// 6. The blank flag is set after a block-level element
			{COMPAT, "<div></div> ", "<div></div> "},
			// 7. Blank detection between two block-level elements
			{COMPAT, "<div></div> <div></div>", "<div></div> <div></div>"},
			// 8. But p-wrapping of non-blank content works after an element
			{COMPAT, "<div></div>x", "<div></div><p>x</p>"},
			// 9. p-wrapping between two block-level elements
			{COMPAT, "<div></div>x<div></div>", "<div></div><p>x</p><div></div>"},
			// 10. p-wrap inside blockquote
			{COMPAT, "<blockquote>x</blockquote>", "<blockquote><p>x</p></blockquote>"},
			// 11. A comment is blank for p-wrapping purposes
			{COMPAT, "<!-- x -->", "<!-- x -->"},
			// 12. A comment is blank even when a p-wrap was opened by a text node
			{COMPAT, " <!-- x -->", " <!-- x -->"},
			// 13. A comment does not open a p-wrap
			{COMPAT, "<!-- x -->x", "<!-- x --><p>x</p>"},
			// 14. A comment does not close a p-wrap
			{COMPAT, "x<!-- x -->", "<p>x<!-- x --></p>"},
			// 15. Empty li
			{COMPAT, "<ul><li></li></ul>", "<ul><li class=\"mw-empty-elt\"></li></ul>"},
			// 16. li with element
			{COMPAT, "<ul><li><span></span></li></ul>", "<ul><li><span></span></li></ul>"},
			// 17. li with text
			{COMPAT, "<ul><li>x</li></ul>", "<ul><li>x</li></ul>"},
			// 18. Empty tr
			{COMPAT, "<table><tbody><tr></tr></tbody></table>",
				"<table><tbody><tr class=\"mw-empty-elt\"></tr></tbody></table>"},
			// 19. Empty p
			{COMPAT, "<p>\n</p>", "<p class=\"mw-empty-elt\">\n</p>"},
		});
	}

	public DepuratorTest(boolean compat_, String input_, String expected_) {
		compat = compat_;
		input = input_;
		expected = expected_;
	}

	@Test
	public void test() throws Exception {
		String prefix = "<html><head></head><body>";
		String suffix = "</body></html>";
		StringReader sr = new StringReader(prefix + input + suffix);
		InputSource source = new InputSource(sr);
		byte[] buffer = Depurator.depurate(source, compat);
		String html = new String(buffer, Charset.forName("UTF-8"));
		Assert.assertTrue(html.startsWith(prefix));
		Assert.assertTrue(html.endsWith(suffix));
		Assert.assertEquals(expected, html.substring(prefix.length(), html.length() - suffix.length()));
	}
}
