/*
 * License: http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.wikimedia.html5depurate;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;
import java.io.InputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.nio.charset.Charset;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nu.validator.encoding.Encoding;

@MultipartConfig()
public class DepurateServlet extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		// Set up charset
		Charset utf8;
		try {
			utf8 = Charset.forName("UTF-8");
		} catch (IllegalArgumentException e) {
			throw new ServletException("No UTF-8", e);
		}

		req.setCharacterEncoding("UTF-8");

		// Make or get the input stream
		InputStream stream = null;
		Part part = req.getPart("text");
		if (part != null) {
			stream = part.getInputStream();
		} else {
			String text = req.getParameter("text");
			if (text != null) {
				byte[] buffer = req.getParameter("text").getBytes(utf8);
				stream = new ByteArrayInputStream(buffer);
			}
		}
		if (stream == null) {
			res.sendError(HttpServletResponse.SC_BAD_REQUEST , "The text parameter must be given");
			return;
		}

		InputSource source = new InputSource(stream);
		source.setEncoding("UTF-8");
		byte[] outputBytes;
		try {
			outputBytes = Depurator.depurate(source);
		} catch (SAXException e) {
			throw new ServletException("Error parsing HTML", e);
		}

		// HtmlSerializer writes UTF-8 by default
		res.setContentType("text/html;charset=UTF-8");
		res.setContentLength(outputBytes.length);
		res.getOutputStream().write(outputBytes);
	}
};
