package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

@ExtendWith(MockitoExtension.class)
class CheckDiscEndpointScopesSupportedSyntax_UnitTest {

	private CheckDiscEndpointScopesSupportedSyntax cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckDiscEndpointScopesSupportedSyntax();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	void acceptsTypicalScopesSupported() {
		putServerMetadata("""
			{
			  "scopes_supported": ["openid", "profile", "offline_access"]
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void noErrorWhenScopesSupportedAbsent() {
		putServerMetadata("""
			{
			  "issuer": "https://as.example.com"
			}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsScopesSupportedWithInvalidEntry() {
		putServerMetadata("""
			{
			  "scopes_supported": ["openid", "two words"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsScopesSupportedNotAnArray() {
		putServerMetadata("""
			{
			  "scopes_supported": "openid profile"
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsScopesSupportedWithDoubleQuote() {
		putServerMetadata("""
			{
			  "scopes_supported": ["fo\\"o"]
			}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsExplicitNull() {
		// null is non-conformant for the optional scopes_supported array and must be flagged.
		putServerMetadata("""
			{"scopes_supported": null}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putServerMetadata(String json) {
		JsonObject metadata = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("server", metadata);
	}
}
