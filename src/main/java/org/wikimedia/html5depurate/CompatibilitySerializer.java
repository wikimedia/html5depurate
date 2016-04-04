package org.wikimedia.html5depurate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Stack;
import java.util.EmptyStackException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

public class CompatibilitySerializer implements ContentHandler, LexicalHandler {

	protected class StackEntry {
		public String uri;
		public String localName;
		public String qName;
		public Attributes attrs;
		OutputStream stream;
		public boolean needsPWrapping;
		public boolean hasOpenPTag;
		public boolean blank;

		public StackEntry(String uri_, String localName_, String qName_,
				Attributes attrs_, OutputStream stream_) {
			uri = uri_;
			localName = localName_;
			qName = qName_;
			attrs = attrs_;
			stream = stream_;
			needsPWrapping = "body".equals(localName_)
				|| "blockquote".equals(localName_);
			hasOpenPTag = false;
			blank = true;
		}
	}

	protected Stack<StackEntry> m_stack;
	protected DepurateSerializer m_serializer;

	protected static final String[] ONLY_INLINE_ELEMENTS = {"a", "abbr", "acronym",
		"applet", "b", "basefont", "bdo", "big", "br", "button", "cite",
		"code", "dfn", "em", "font", "i", "iframe", "img", "input", "kbd",
		"label", "legend", "map", "object", "param", "q", "rb", "rbc", "rp",
		"rt", "rtc", "ruby", "s", "samp", "select", "small", "span", "strike",
		"strong", "sub", "sup", "textarea", "tt", "u", "var"};

	public CompatibilitySerializer(OutputStream out) {
		m_stack = new Stack<StackEntry>();
		m_serializer = new DepurateSerializer(out);
	}

	private StackEntry peek() throws SAXException {
		try {
			return m_stack.peek();
		} catch (EmptyStackException e) {
			return null;
		}
	}

	private StackEntry pop() throws SAXException {
		try {
			return m_stack.pop();
		} catch (EmptyStackException e) {
			throw new SAXException(e);
		}
	}

	public void characters(char[] chars, int start, int length)
			throws SAXException {
		StackEntry entry = peek();
		if (entry != null) {
			if (entry.needsPWrapping && !entry.hasOpenPTag) {
				m_serializer.write("<p>");
				entry.hasOpenPTag = true;
			}
			if (entry.blank) {
				for (int i = start; i < start + length; i++) {
					char c = chars[i];
					if (!(c == 9 || c == 10 || c == 12 || c == 13 || c == 32)) {
						entry.blank = false;
						break;
					}
				}
			}
		}
		m_serializer.characters(chars, start, length);
	}

	private boolean isOnlyInline(String localName) {
		return Arrays.binarySearch(ONLY_INLINE_ELEMENTS, localName) > -1;
	}

	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		StackEntry oldEntry = peek();
		if (oldEntry != null) {
			oldEntry.blank = false;
			if (oldEntry.hasOpenPTag) {
				if (!isOnlyInline(localName)) {
					m_serializer.write("</p>");
					oldEntry.hasOpenPTag = false;
				}
			} else if (oldEntry.needsPWrapping && isOnlyInline(localName)) {
				m_serializer.write("<p>");
				oldEntry.hasOpenPTag = true;
			}
		}
		m_stack.push(new StackEntry(uri, localName, qName, atts,
					m_serializer.getOutputStream()));
		m_serializer.setOutputStream(new ByteArrayOutputStream());
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		StackEntry entry = pop();

		// Annotate empty tr and li elements so that they can be hidden in CSS,
		// for compatibility with tidy and existing wikitext
		if ("tr".equals(localName) || "li".equals(localName)) {
			if (entry.attrs.getLength() == 0 && entry.blank) {
				AttributesImpl newAttrs = new AttributesImpl();
				newAttrs.addAttribute("", "class", "class", "", "mw-empty-" + localName);
				entry.attrs = newAttrs;
			}
		}
		ByteArrayOutputStream oldStream = (ByteArrayOutputStream)m_serializer.getOutputStream();
		m_serializer.setOutputStream(entry.stream);
		m_serializer.startElement(entry.uri, entry.localName, entry.qName, entry.attrs);
		m_serializer.writeStream(oldStream);
		if (entry.hasOpenPTag) {
			m_serializer.write("</p>");
		}
		m_serializer.endElement(uri, localName, qName);
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
		m_serializer.endDocument();
	}

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		characters(ch, start, length);
	}
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void comment(char[] ch, int start, int length) throws SAXException {
		StackEntry entry = peek();
		if (entry != null) {
			entry.blank = false;
		}
		m_serializer.comment(ch, start, length);
	}

	public void endCDATA() throws SAXException {
	}
	public void endDTD() throws SAXException {
	}

	public void endEntity(String name) throws SAXException {
	}

	public void startCDATA() throws SAXException {
	}

	public void startDTD(String name, String publicId, String systemId)
			throws SAXException {
	}

	public void startEntity(String name) throws SAXException {
	}

	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void skippedEntity(String name) throws SAXException {
	}
}
