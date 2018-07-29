
package io.fintechlabs.testframework;

import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.connector.Connector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class Application {

	private static class EventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
		final private static org.slf4j.Logger logger = LoggerFactory.getLogger(EventListener.class);

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEvent) {
			// This redirects port 8443 on localhost to the same port on the ingress (httpd).
			// This is so that selenium running on this machine can make submissions via the ingress
			// when a developer is running the conformance suite locally, as otherwise accesses to
			// http://localhost:8443/ will fail.
			// (when deployed in the cloud, selenium will be sent to the external hostname & IP for the
			// conformance suite which will work fine - it's only when the hostname resolves 127.0.0.1
			// that the problem happens.)
			String key = "fintechlabs.startredir";
			ConfigurableEnvironment env = applicationEvent.getEnvironment();
			if (env.containsProperty(key)) {
				boolean startRedir = env.getProperty(key, boolean.class);
				if (startRedir) {
					try {
						logger.info(key + ": true - launching redir process");
						Process process = new ProcessBuilder("redir", ":8443", "httpd:8443").start();
						// redir immediately forks into a daemon so there's no need to read it's I/O streams
						process.waitFor();
					} catch (Exception err) {
						logger.error("launching redir process failed");
						err.printStackTrace();
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		Security.addProvider(new BouncyCastleProvider());

		// quiet down CSS errors/warnings in the parser selenium/htmlunit uses
		Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);

		SpringApplication springApplication = new SpringApplication(Application.class);
		springApplication.addListeners(new EventListener());
		springApplication.run(args);
	}

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {

		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

		Connector ajpConnector = new Connector("AJP/1.3");
		// ajpConnector.setProtocol("AJP/1.3");
		ajpConnector.setPort(9090);
		ajpConnector.setSecure(false);
		ajpConnector.setAllowTrace(false);
		ajpConnector.setScheme("http");
		tomcat.addAdditionalTomcatConnectors(ajpConnector);

		return tomcat;
	}
}
