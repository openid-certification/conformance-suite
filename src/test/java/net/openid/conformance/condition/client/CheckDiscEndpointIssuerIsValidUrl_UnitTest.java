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
class CheckDiscEndpointIssuerIsValidUrl_UnitTest {

	private CheckDiscEndpointIssuerIsValidUrl cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckDiscEndpointIssuerIsValidUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	private void putIssuer(String json) {
		env.putObject("server", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void acceptsPlainHttpsIssuer() {
		putIssuer("""
			{"issuer": "https://as.example.com"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsIssuerWithPath() {
		putIssuer("""
			{"issuer": "https://as.example.com/tenant1"}
			""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void rejectsHttpIssuer() {
		putIssuer("""
			{"issuer": "http://as.example.com"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsIssuerWithQuery() {
		putIssuer("""
			{"issuer": "https://as.example.com?x=y"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsIssuerWithFragment() {
		putIssuer("""
			{"issuer": "https://as.example.com#frag"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsMissingIssuer() {
		putIssuer("""
			{"token_endpoint": "https://as.example.com/token"}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	void rejectsNonStringIssuer() {
		putIssuer("""
			{"issuer": 42}
			""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
