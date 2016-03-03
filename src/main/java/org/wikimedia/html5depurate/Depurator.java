package org.wikimedia.html5depurate;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;


class Depurator {
	public static byte[] depurate(InputSource source, boolean compat)
		throws SAXException, IOException
	{
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ContentHandler serializer;
		if (compat) {
			serializer = new CompatibilitySerializer(sink);
		} else {
			serializer = new DepurateSerializer(sink);
		}
		HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALLOW);
		parser.setContentHandler(serializer);
		parser.setProperty("http://xml.org/sax/properties/lexical-handler",
				serializer);
		parser.parse(source);
		return sink.toByteArray();
	}
}
