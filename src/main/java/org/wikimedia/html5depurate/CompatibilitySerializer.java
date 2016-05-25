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
		public boolean isPWrapper;
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
			blank = true;
			isPWrapper = "mw:p-wrap".equals(localName_);
		}
	}

	protected Stack<StackEntry> m_stack;
	protected DepurateSerializer m_serializer;

	// Warning: this list must be in alphabetical order
	protected static final String[] ONLY_INLINE_ELEMENTS = {"a", "abbr", "acronym",
		"applet", "b", "basefont", "bdo", "big", "br", "button", "cite",
		"code", "dfn", "em", "font", "i", "iframe", "img", "input", "kbd",
		"label", "legend", "map", "object", "param", "q", "rb", "rbc", "rp",
		"rt", "rtc", "ruby", "s", "samp", "select", "small", "span", "strike",
		"strong", "sub", "sup", "textarea", "tt", "u", "var"};

	// Warning: this list must be in alphabetical order
	protected static final String[] MARKED_EMPTY_ELEMENTS = {"li", "p", "tr"};

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

	/**
	 * Pop the top of the stack, restore the parent stream in the serializer
	 * and return the previous stream
	 */
	private ByteArrayOutputStream popAndGetContents() throws SAXException {
		try {
			StackEntry entry = m_stack.pop();
			ByteArrayOutputStream oldStream =
				(ByteArrayOutputStream)m_serializer.getOutputStream();
			m_serializer.setOutputStream(entry.stream);
			return oldStream;
		} catch (EmptyStackException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Push a new element to the top of the stack, and set up a new empty
	 * stream in the serializer. Returns the new element.
	 */
	private StackEntry push(String uri, String localName, String qName,
				Attributes attrs) throws SAXException {
		StackEntry entry = new StackEntry(uri, localName, qName, attrs,
				m_serializer.getOutputStream());
		m_stack.push(entry);
		m_serializer.setOutputStream(new ByteArrayOutputStream());
		return entry;
	}

	/**
	 * Equivalent to push() for a proposed p element. Will become a real
	 * p element if the contents is non-blank.
	 */
	private StackEntry pushPWrapper() throws SAXException {
		return push("", "mw:p-wrap", "mw:p-wrap", new AttributesImpl());
	}

	private void writePWrapper(StackEntry entry, ByteArrayOutputStream contents)
			throws SAXException {
		if (!entry.blank) {
			m_serializer.write("<p>");
			m_serializer.writeStream(contents);
			m_serializer.write("</p>");
		} else {
			m_serializer.writeStream(contents);
		}
	}

	public void characters(char[] chars, int start, int length)
			throws SAXException {
		StackEntry entry = peek();
		if (entry != null) {
			if (entry.needsPWrapping) {
				entry = pushPWrapper();
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
			if (oldEntry.isPWrapper) {
				if (!isOnlyInline(localName)) {
					// This is non-inline so close the p-wrapper
					ByteArrayOutputStream contents = popAndGetContents();
					writePWrapper(oldEntry, contents);
					oldEntry = peek();
				} else {
					// We're putting an element inside the p-wrapper, so it is non-blank now
					oldEntry.blank = false;
				}
			} else {
				oldEntry.blank = false;
			}
		}
		if (oldEntry != null && oldEntry.needsPWrapping && isOnlyInline(localName)) {
			StackEntry entry = pushPWrapper();
			// We're putting an element inside the p-wrapper, so it is non-blank
			entry.blank = false;
		}
		push(uri, localName, qName, atts);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		StackEntry entry = peek();
		ByteArrayOutputStream contents = popAndGetContents();

		if (entry.isPWrapper) {
			// Since we made this p-wrapper, the caller really wants to end the parent element.
			// So first we need to close the p-wrapper
			writePWrapper(entry, contents);
			entry = peek();
			contents = popAndGetContents();
		}

		// Annotate empty tr and li elements so that they can be hidden in CSS,
		// for compatibility with tidy and existing wikitext
		if (Arrays.binarySearch(MARKED_EMPTY_ELEMENTS, localName) > -1) {
			if (entry.attrs.getLength() == 0 && entry.blank) {
				AttributesImpl newAttrs = new AttributesImpl();
				newAttrs.addAttribute("", "class", "class", "", "mw-empty-elt");
				entry.attrs = newAttrs;
			}
		}
		m_serializer.startElement(entry.uri, entry.localName, entry.qName, entry.attrs);
		m_serializer.writeStream(contents);
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
