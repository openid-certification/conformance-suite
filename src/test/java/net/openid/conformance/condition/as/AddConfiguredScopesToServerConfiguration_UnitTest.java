package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AddConfiguredScopesToServerConfiguration_UnitTest {

	private AddConfiguredScopesToServerConfiguration cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new AddConfiguredScopesToServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void unionsTheScopesOfBothClientsWithoutDuplicates() {
		setUpEnvironment("""
			{"client": {"scope": "openid accounts"}, "client2": {"scope": "openid payments"}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		cond.execute(env);

		assertEquals(List.of("openid", "accounts", "payments"), scopesSupported());
	}

	@Test
	void createsScopesSupportedWhenTheServerHasNone() {
		setUpEnvironment("""
			{"client": {"scope": "accounts"}}
			""", """
			{"issuer": "https://as.example.com"}
			""");

		cond.execute(env);

		assertEquals(List.of("accounts"), scopesSupported());
	}

	@Test
	void doesNotPublishAnEmptyArrayWhenNothingIsConfigured() {
		setUpEnvironment("""
			{"client": {"client_id": "foo"}}
			""", """
			{"issuer": "https://as.example.com"}
			""");

		cond.execute(env);

		assertFalse(env.getObject("server").has("scopes_supported"));
	}

	@Test
	void addsConfiguredScopesAfterTheExistingOnes() {
		setUpEnvironment("""
			{"client": {"scope": "openid accounts consents"}}
			""", """
			{"scopes_supported": ["openid", "phone", "profile", "email", "address",
			 "offline_access", "consents", "resources", "payments"]}
			""");

		cond.execute(env);

		assertEquals(List.of("openid", "phone", "profile", "email", "address", "offline_access",
			"consents", "resources", "payments", "accounts"), scopesSupported());
	}

	@Test
	void toleratesExtraWhitespaceInTheConfiguredScope() {
		setUpEnvironment("""
			{"client": {"scope": "  openid   accounts  "}}
			""", """
			{"issuer": "https://as.example.com"}
			""");

		cond.execute(env);

		assertEquals(List.of("openid", "accounts"), scopesSupported());
	}

	@Test
	void leavesTheServerAloneWhenThereIsNoClientConfiguration() {
		setUpEnvironment("""
			{"server": {"discoveryUrl": "https://as.example.com"}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		cond.execute(env);

		assertEquals(List.of("openid"), scopesSupported());
	}

	@Test
	void rejectsAScopeThatIsNotAString() {
		setUpEnvironment("""
			{"client": {"scope": ["openid", "accounts"]}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAnExistingScopesSupportedThatIsNotAnArray() {
		setUpEnvironment("""
			{"client": {"scope": "openid"}}
			""", """
			{"scopes_supported": "openid accounts"}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setUpEnvironment(String config, String server) {
		env.putObject("config", JsonParser.parseString(config).getAsJsonObject());
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
	}

	private List<String> scopesSupported() {
		JsonObject server = env.getObject("server");
		return OIDFJSON.convertJsonArrayToList(server.getAsJsonArray("scopes_supported"));
	}
}
