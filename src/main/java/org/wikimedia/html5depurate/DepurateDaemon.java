package org.wikimedia.html5depurate;

import org.wikimedia.html5depurate.Config;
import org.wikimedia.html5depurate.DepurateErrorPageGenerator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Daemon for execution via jsvc
 */
public class DepurateDaemon implements Daemon {
	HttpServer m_server;
	String[] m_args;
	Logger m_logger = Logger.getLogger(this.getClass().getName());

	public static void main(String[] args) throws Exception {
		DepurateDaemon daemon = new DepurateDaemon();
		daemon.m_args = args;
		daemon.start();
		Thread.currentThread().join();
	}

	public void init(DaemonContext context)
			throws DaemonInitException,Exception
	{
		m_args = context.getArguments();
	}

	protected CommandLine loadCommandLine()
			throws IOException, ParseException
	{
		Options options = new Options();
		options.addOption("c", true, "The configuration file name");
		DefaultParser parser = new DefaultParser();
		return parser.parse(options, m_args);
	}

	protected Config loadConfig(String path) {
		Config config = new Config();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			m_logger.warning("Config file not found: " + path);
		} catch (IOException e) {
			m_logger.warning("Error loading config file: " + e.toString());
		}

		double maxPostSize = Double.parseDouble(
				properties.getProperty("maxPostSize", "100e6"));
		if (maxPostSize > Integer.MAX_VALUE) {
			config.maxPostSize = Integer.MAX_VALUE;
		} else if (maxPostSize > 0) {
			config.maxPostSize = (int)maxPostSize;
		} else {
			config.maxPostSize = 100000000;
		}
		m_logger.info("Max post size: " + config.maxPostSize);

		config.host = properties.getProperty("host", "localhost");
		config.port = Integer.parseInt(properties.getProperty("port", "4339"));
		m_logger.info("Binding to " + config.host + ":" + Integer.toString(config.port));

		return config;
	}

	public void start() throws Exception {
		m_logger.info("Starting");

		CommandLine cl = loadCommandLine();
		String configPath = "/etc/html5depurate/html5depurate.conf";
		if (cl.hasOption("c")) {
			configPath = cl.getOptionValue("c");
		}
		Config config = loadConfig(configPath);

		m_server = new HttpServer();
		m_server.addListener(
				new NetworkListener("depurate", config.host, config.port));

		ServerConfiguration serverConf = m_server.getServerConfiguration();
		serverConf.addHttpHandler(new DepurateHandler(config), "/document", "/body");
		serverConf.setDefaultErrorPageGenerator(new DepurateErrorPageGenerator());
		serverConf.setName("depurate");
		m_server.start();
	}

	public void stop() throws Exception {
		m_logger.info("Stopping");
		if (m_server != null) {
			m_server.shutdownNow();
		}
	}

	public void destroy() {
	}
}
