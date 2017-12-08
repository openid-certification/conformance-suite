
package io.fintechlabs.testframework;

import java.security.Security;

import org.apache.catalina.connector.Connector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		
		Security.addProvider(new BouncyCastleProvider());
		
		SpringApplication.run(Application.class, args);
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
