package net.openid.conformance.openid.federation;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the value checks ValidateUrlRequirements makes before parsing the URL, through
 * ValidateEntityIdentifier: a missing or JSON null value is reported as missing, and any other
 * non-string as not a string, rather than as an exception.
 */
class ValidateEntityIdentifier_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateEntityIdentifier cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateEntityIdentifier();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putEntityIdentifier(String jsonValue) {
		env.putObject("config", JsonParser.parseString(
			"{ \"federation\": { \"entity_identifier\": " + jsonValue + " } }").getAsJsonObject());
	}

	@Test
	public void httpsUrl_succeeds() {
		putEntityIdentifier("\"https://rp.example.com\"");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingValue_isReportedAsMissing() {
		env.putObject("config", JsonParser.parseString("{ \"federation\": { } }").getAsJsonObject());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is missing or null"), e.getMessage());
	}

	@Test
	public void jsonNull_isReportedAsMissing() {
		putEntityIdentifier("null");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is missing or null"), e.getMessage());
	}

	@Test
	public void object_isReportedAsNotAString() {
		putEntityIdentifier("{ \"url\": \"https://rp.example.com\" }");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a string"), e.getMessage());
	}

	@Test
	public void number_isReportedAsNotAString() {
		putEntityIdentifier("42");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a string"), e.getMessage());
	}
}
