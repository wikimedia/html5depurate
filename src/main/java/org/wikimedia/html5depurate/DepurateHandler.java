package org.wikimedia.html5depurate;

import org.wikimedia.html5depurate.Config;
import org.wikimedia.html5depurate.MultipartBuffer;
import org.wikimedia.html5depurate.Depurator;

import org.glassfish.grizzly.http.multipart.MultipartScanner;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.EmptyCompletionHandler;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class DepurateHandler extends HttpHandler {
	final private Config m_config;
	Logger m_logger = Logger.getLogger(this.getClass().getName());


	DepurateHandler(Config config) {
		super("depurate");
		m_config = config;
	}

	@Override
	public void service(final Request request, final Response response)
			throws Exception
	{
		m_logger.finer("Request received");

		String path = request.getHttpHandlerPath();

		boolean compat_ = false;
		if (path.equals("/document")) {
			compat_ = false;
		} else if (path.equals("/compat/document")) {
			compat_ = true;
		} else {
			m_logger.log(Level.INFO, "Unknown API path: {0}", path);
			sendError(response, 404, "Unknown API path");
			return;
		}
		// compat must be final to be passed to the closure
		final boolean compat = compat_;

		response.suspend();
		request.setCharacterEncoding("UTF-8");
		final MultipartBuffer buf = new MultipartBuffer(m_config.maxPostSize);

		MultipartScanner.scan(request,
			buf,
			new EmptyCompletionHandler<Request>() {
				private boolean m_done = false;

				@Override
				public void completed(final Request request) {
					m_logger.finer("Multipart complete");
					continueRequest(request);
				}

				@Override
				public void failed(Throwable throwable) {
					m_logger.finer("Multipart failed");
					continueRequest(request);
				}

				private void continueRequest(final Request request) {
					// MultipartReadHandler normally calls us 3 times. This is apparently a bug.
					if (!m_done) {
						m_done = true;
						depurate(request, response, buf, compat);
						response.resume();
					}
				}
			}
		);
	}

	private void depurate(
			final Request request, Response response, MultipartBuffer multi, boolean compat)
	{
		try {
			if (multi.isTooBig()) {
				sendError(response, 400, "The POST size was too large");
				return;
			}
			String text = request.getParameter("text");
			InputSource source = null;
			if (text != null) {
				StringReader sr = new StringReader(text);
				source = new InputSource(sr);
				m_logger.log(Level.INFO, "Depurating {0} chars of URL input",
						text.length());
			} else {
				byte[] textBytes = multi.getParameter("text");
				if (textBytes != null) {
					m_logger.log(Level.INFO, 
							"Depurating {0} bytes of multipart input",
							textBytes.length);
					InputStream stream = new ByteArrayInputStream(textBytes);
					source = new InputSource(stream);
					source.setEncoding("UTF-8");
				}
			}
			if (source == null) {
				sendError(response, 400, "The text parameter must be given");
				return;
			}

			byte[] outputBytes = {};
			try {
				outputBytes = Depurator.depurate(source, compat);
			} catch (SAXException e) {
				m_logger.info("Error running depurator");
				sendError(response, 500, "Error while parsing HTML: " + e.toString());
				return;
			}

			response.setContentType("text/html;charset=UTF-8");
			response.setContentLength(outputBytes.length);
			response.setBufferSize(outputBytes.length);
			response.getOutputStream().write(outputBytes);
		} catch (IOException e) {
			m_logger.warning("Got IOException: " + e.toString());
			sendError(response, 500, "Got IOException: " + e.toString());
		} catch (Exception e) {
			m_logger.warning("Got unexpected exception: " + e.toString());
			sendError(response, 500, "Unexpected exception: " +
					e.toString());
		}
	}

	private void sendError(Response response, int code, String message) {
		response.getResponse().setAllowCustomReasonPhrase(false);
		try {
			response.sendError(code, message);
		} catch (IOException e) {
			m_logger.warning("Got IOException while sending error: " + e.toString());
		}
	}
}
