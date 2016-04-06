package org.wikimedia.html5depurate;

import java.io.PrintWriter;
import java.io.StringWriter;

class Util {
	public static String format(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
