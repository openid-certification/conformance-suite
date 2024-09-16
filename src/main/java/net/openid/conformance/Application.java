package net.openid.conformance;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.token.TokenService;
import net.openid.conformance.ui.ServerInfoTemplate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;

import jakarta.annotation.PostConstruct;
import java.security.Security;

@SpringBootApplication
public class Application {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Application.class);

	@Autowired
	private ServerInfoTemplate serverInfoTemplate;

	@Autowired
	private TestInfoService testInfoService;

	@Autowired
	private TestPlanService testPlanService;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private EventLog eventLog;

	@Autowired
	private MongoClient mongoClient;

	@Value("${openid.mongodb.targetFeatureCompatibilityVersion:}")
	private String targetFeatureCompatibilityVersion;

	@EventListener(ApplicationReadyEvent.class)
	public void setMongoFeatureCompatibilityVersion() {
		MongoDatabase adminDb = mongoClient.getDatabase("admin");

		String currentVersion = getCurrentFeatureCompatibilityVersion(adminDb);
		logger.info("mongodb server version is '%s', featureCompatibilityVersion is currently '%s' and openid.mongodb.targetFeatureCompatibilityVersion is '%s'".formatted(
			getMongoDBVersion(adminDb),
			currentVersion,
			targetFeatureCompatibilityVersion));

		if (!targetFeatureCompatibilityVersion.isBlank() &&
				!currentVersion.equals(targetFeatureCompatibilityVersion)) {
			Document command = new Document("setFeatureCompatibilityVersion", targetFeatureCompatibilityVersion);
			mongoClient.getDatabase("admin").runCommand(command);
			logger.info("mongodb command setFeatureCompatibilityVersion " + targetFeatureCompatibilityVersion + " executed successfully");
		}
	}

	private static String getMongoDBVersion(MongoDatabase adminDb) {
		Document command = new Document("buildInfo", 1);
		Document result = adminDb.runCommand(command);
		return result.getString("version");
	}

	private static String getCurrentFeatureCompatibilityVersion(MongoDatabase adminDb) {
		Document command = new Document("getParameter", 1);
		command.put("featureCompatibilityVersion", 1);
		Document result = adminDb.runCommand(command);
		Document featureCompatibilityVersion = result.get("featureCompatibilityVersion", Document.class);
		String currentVersion = featureCompatibilityVersion.getString("version");
		return currentVersion;
	}

	private static class PreparedEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
		private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PreparedEventListener.class);

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEvent) {
			startRedir(applicationEvent);
		}

		private static void startRedir(ApplicationEnvironmentPreparedEvent applicationEvent) {
			// This redirects port 8443 on localhost to the same port on the ingress (httpd).
			// This is so that selenium running on this machine can make submissions via the ingress
			// when a developer is running the conformance suite locally, as otherwise accesses to
			// https://localhost:8443/ will fail.
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
						logger.error("launching redir process failed", err);
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		Security.addProvider(new BouncyCastleProvider());

		SpringApplication springApplication = new SpringApplication(Application.class);
		springApplication.addListeners(new PreparedEventListener());
		springApplication.run(args);
	}

	@Bean
	public TomcatServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		return tomcat;
	}

	@PostConstruct
	private void doPostConstruct() {
		serverInfoTemplate.initServerInfo();
		testInfoService.createIndexes();
		testPlanService.createIndexes();
		tokenService.createIndexes();
		eventLog.createIndexes();
	}
}
