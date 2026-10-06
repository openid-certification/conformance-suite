package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidatePDPIdentifier_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPIdentifier cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPIdentifier();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putIdentifier(String value) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (value != null) {
			pdp.addProperty("policy_decision_point", value);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	@Test
	public void validHttpsUrl_succeeds() {
		putIdentifier("https://pdp.example.com");
		cond.execute(env);
	}

	@Test
	public void missingIdentifier_fails() {
		putIdentifier(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpScheme_fails() {
		putIdentifier("http://pdp.example.com");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void withQuery_fails() {
		putIdentifier("https://pdp.example.com/path?foo=bar");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void withFragment_fails() {
		putIdentifier("https://pdp.example.com/path#frag");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putIdentifierElement(JsonElement value) {
		JsonObject pdp = new JsonObject();
		pdp.add("policy_decision_point", value);
		JsonObject config = new JsonObject();
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	@Test
	public void objectIdentifier_failsAsNotAString() {
		putIdentifierElement(JsonParser.parseString("{ \"url\": \"https://pdp.example.com\" }"));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a string"));
	}

	@Test
	public void arrayIdentifier_failsAsNotAString() {
		putIdentifierElement(JsonParser.parseString("[ \"https://pdp.example.com\" ]"));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a string"));
	}

	@Test
	public void numericIdentifier_failsAsNotAString() {
		putIdentifierElement(JsonParser.parseString("42"));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a string"));
	}

	@Test
	public void jsonNullIdentifier_failsAsMissing() {
		putIdentifierElement(JsonNull.INSTANCE);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is missing or null"));
	}
}
