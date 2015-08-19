package org.wikimedia.html5depurate;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.htmlparser.sax.HtmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;


class Depurator {
	public static byte[] depurate(InputSource source)
		throws SAXException, IOException
	{
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ContentHandler serializer = new HtmlSerializer(sink);
		HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALLOW);
		parser.setContentHandler(serializer);
		parser.setProperty("http://xml.org/sax/properties/lexical-handler",
				serializer);
		parser.parse(source);
		return sink.toByteArray();
	}
}
