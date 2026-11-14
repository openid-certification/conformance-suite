package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnAuthzenSearchResultEmptyValues_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenSearchResultEmptyValues cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenSearchResultEmptyValues();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void respond(String results) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString("{ \"results\": " + results + " }").getAsJsonObject());
	}

	@Test
	public void nonEmptyValues_succeed() {
		respond("[ { \"type\": \"user\", \"id\": \"alice\" }, { \"name\": \"read\" } ]");
		cond.execute(env);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"[ { \"type\": \"user\", \"id\": \"\" } ]",
		"[ { \"type\": \"\", \"id\": \"alice\" } ]",
		"[ { \"name\": \"\" } ]"
	})
	public void emptyValue_isFlagged(String results) {
		respond(results);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("type, id or name is an empty string"), e.getMessage());
	}

	@Test
	public void missingOrNonStringValues_areLeftToTheValidators() {
		respond("[ { \"type\": \"user\" }, { \"type\": \"user\", \"id\": 42 }, \"not-an-object\" ]");
		cond.execute(env);
	}

	@Test
	public void noResultsArray_succeeds() {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString("{}").getAsJsonObject());
		cond.execute(env);
	}
}
