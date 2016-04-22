package org.wikimedia.html5depurate;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.DefaultErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.util.HttpStatus;

public class DepurateErrorPageGenerator implements ErrorPageGenerator {
	DefaultErrorPageGenerator def = new DefaultErrorPageGenerator();

	public String generate(final Request request,
			final int status, final String reasonPhrase,
			final String description, final Throwable exception)
	{
		String realReasonPhrase;
		if (reasonPhrase.equals(description)) {
			realReasonPhrase = HttpStatus.getHttpStatus(status).getReasonPhrase();
		} else {
			realReasonPhrase = reasonPhrase;
		}
		return def.generate(request, status,
				"\n" + realReasonPhrase + "\n",
				"\n" + description + "\n",
				exception);
	}
}
