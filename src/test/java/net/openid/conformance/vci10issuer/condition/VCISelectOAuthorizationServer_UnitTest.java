package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VCISelectOAuthorizationServer_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		env = new Environment();
		env.putObject("config", new JsonObject());
	}

	private void run() {
		VCISelectOAuthorizationServer cond = new VCISelectOAuthorizationServer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		cond.execute(env);
	}

	/** Stores the issuer metadata and, for each server in {@code servers}, metadata whose issuer is that server. */
	private void put(String credentialIssuerMetadataJson, String... servers) {
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", JsonParser.parseString(credentialIssuerMetadataJson).getAsJsonObject());
		JsonObject authorizationServers = new JsonObject();
		for (int i = 0; i < servers.length; i++) {
			JsonObject entry = new JsonObject();
			JsonObject metadata = new JsonObject();
			metadata.addProperty("issuer", servers[i]);
			entry.add("authorization_server_metadata", metadata);
			authorizationServers.add("server" + i, entry);
		}
		vci.add("authorization_servers", authorizationServers);
		env.putObject("vci", vci);
	}

	private void configureAuthorizationServer(String value) {
		JsonObject vciConfig = new JsonObject();
		vciConfig.addProperty("authorization_server", value);
		env.getObject("config").add("vci", vciConfig);
	}

	private String selectedIssuer() {
		return env.getString("server", "issuer");
	}

	@Test
	void selectsTheOnlyListedServerWhenItIsNotTheCredentialIssuer() {
		put("""
			{"credential_issuer": "https://issuer.example.com", "authorization_servers": ["https://as.example.com"]}
			""", "https://as.example.com");
		run();
		assertEquals("https://as.example.com", selectedIssuer());
	}

	@Test
	void treatsAnEmptyConfiguredServerAsNotConfigured() {
		put("""
			{"credential_issuer": "https://issuer.example.com", "authorization_servers": ["https://as.example.com"]}
			""", "https://as.example.com");
		configureAuthorizationServer("");
		run();
		assertEquals("https://as.example.com", selectedIssuer());
	}

	@Test
	void usesTheCredentialIssuerWhenNoServersAreListed() {
		put("""
			{"credential_issuer": "https://issuer.example.com"}
			""", "https://issuer.example.com");
		run();
		assertEquals("https://issuer.example.com", selectedIssuer());
	}

	@Test
	void prefersTheCredentialIssuerAmongSeveralListedServers() {
		put("""
			{"credential_issuer": "https://issuer.example.com",
			 "authorization_servers": ["https://as.example.com", "https://issuer.example.com"]}
			""", "https://as.example.com", "https://issuer.example.com");
		run();
		assertEquals("https://issuer.example.com", selectedIssuer());
	}

	@Test
	void asksForTheServerWhenSeveralAreListedWithoutTheCredentialIssuer() {
		put("""
			{"credential_issuer": "https://issuer.example.com",
			 "authorization_servers": ["https://as0.example.com", "https://as1.example.com"]}
			""", "https://as0.example.com", "https://as1.example.com");
		ConditionError e = assertThrows(ConditionError.class, this::run);
		assertTrue(e.getMessage().contains("please specify the one to test in the 'Authorization Server URL' field"));
	}

	@Test
	void selectsTheConfiguredServer() {
		put("""
			{"credential_issuer": "https://issuer.example.com",
			 "authorization_servers": ["https://as0.example.com", "https://as1.example.com"]}
			""", "https://as0.example.com", "https://as1.example.com");
		configureAuthorizationServer("https://as1.example.com");
		run();
		assertEquals("https://as1.example.com", selectedIssuer());
	}

	@Test
	void rejectsAConfiguredServerThatIsNotListed() {
		put("""
			{"credential_issuer": "https://issuer.example.com", "authorization_servers": ["https://as.example.com"]}
			""", "https://as.example.com");
		configureAuthorizationServer("https://other.example.com");
		ConditionError e = assertThrows(ConditionError.class, this::run);
		assertTrue(e.getMessage().contains("The 'Authorization Server URL' in the test configuration is not listed"));
	}
}
