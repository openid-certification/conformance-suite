package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CheckDiscEndpointScopesSupportedContainsConfiguredScopes_UnitTest {

	private CheckDiscEndpointScopesSupportedContainsConfiguredScopes cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckDiscEndpointScopesSupportedContainsConfiguredScopes();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void acceptsAServerThatPublishesMoreThanIsConfigured() {
		setUpEnvironment("""
			{"client": {"scope": "openid"}}
			""", """
			{"scopes_supported": ["openid", "accounts", "payments"]}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsAServerThatDoesNotPublishAConfiguredScope() {
		setUpEnvironment("""
			{"client": {"scope": "openid accounts"}}
			""", """
			{"scopes_supported": ["openid", "payments"]}
			""");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("scopes_supported"));
	}

	@Test
	void checksTheSecondClientToo() {
		setUpEnvironment("""
			{"client": {"scope": "openid"}, "client2": {"scope": "openid payments"}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void acceptsAConfigurationWithNoScope() {
		// eg the ConnectID profile, which hides the field and always requests 'openid'
		setUpEnvironment("""
			{"client": {"client_id": "foo"}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void ignoresPerTransactionScopesAddedToTheClientAtRuntime() {
		setUpEnvironment("""
			{"client": {"scope": "openid consents"}}
			""", """
			{"scopes_supported": ["openid", "consents"]}
			""");
		// the Brazil pre-authorization steps add the consent id to the scope of the client
		// object; no server can publish that, and it must not be checked
		env.putObject("client", JsonParser.parseString("""
			{"scope": "openid consents consent:urn:example:consent:1234"}
			""").getAsJsonObject());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsScopesSupportedThatIsNotAnArray() {
		setUpEnvironment("""
			{"client": {"scope": "openid"}}
			""", """
			{"scopes_supported": "openid"}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAConfiguredScopeThatIsNotAString() {
		setUpEnvironment("""
			{"client": {"scope": ["openid"]}}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setUpEnvironment(String config, String server) {
		env.putObject("config", JsonParser.parseString(config).getAsJsonObject());
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
	}
}
