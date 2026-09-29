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
class CheckDiscEndpointScopesSupportedContainsRequestedScopes_UnitTest {

	private CheckDiscEndpointScopesSupportedContainsRequestedScopes cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckDiscEndpointScopesSupportedContainsRequestedScopes();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void acceptsAServerThatPublishesMoreThanIsRequested() {
		setUpEnvironment("""
			{"scope": "openid"}
			""", """
			{"scopes_supported": ["openid", "accounts", "payments"]}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsAServerThatDoesNotPublishARequestedScope() {
		setUpEnvironment("""
			{"scope": "openid accounts"}
			""", """
			{"scopes_supported": ["openid", "payments"]}
			""");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("scopes_supported"));
	}

	@Test
	void checksTheSecondClientToo() {
		setUpEnvironment("""
			{"scope": "openid"}
			""", """
			{"scopes_supported": ["openid"]}
			""");
		env.putObject("client2", JsonParser.parseString("""
			{"scope": "openid payments"}
			""").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void usesTheScopeOnTheClientRatherThanTheConfiguration() {
		// eg the ConnectID profile, which ignores the configured scope and always requests 'openid'
		env.putObject("config", JsonParser.parseString("""
			{"client": {"scope": "openid accounts"}}
			""").getAsJsonObject());
		setUpEnvironment("""
			{"scope": "openid"}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsAClientWithNoScopeYet() {
		// eg the KSA profile, which only sets the scope once it has created a consent
		setUpEnvironment("""
			{"client_id": "foo"}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsScopesSupportedThatIsNotAnArray() {
		setUpEnvironment("""
			{"scope": "openid"}
			""", """
			{"scopes_supported": "openid"}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsAScopeThatIsNotAString() {
		setUpEnvironment("""
			{"scope": ["openid"]}
			""", """
			{"scopes_supported": ["openid"]}
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setUpEnvironment(String client, String server) {
		env.putObject("client", JsonParser.parseString(client).getAsJsonObject());
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
	}
}
