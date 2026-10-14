package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers ExtractAuthzenDecisionExpectedResponse and EnsureAuthzenDecisionResponseValMatchesExpected.
 */
@ExtendWith(MockitoExtension.class)
class EnsureAuthzenDecisionResponseValMatchesExpected_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	private void expect(String expected, String actual) {
		run(new ExtractAuthzenDecisionExpectedResponse(expected));
		env.putObject("authzen_api_endpoint_decision", json(actual));
	}

	@Test
	public void sameDecision_succeeds() {
		expect("{ \"decision\": false }", "{ \"decision\": false, \"context\": { \"reason\": \"archived\" } }");
		run(new EnsureAuthzenDecisionResponseValMatchesExpected());
	}

	@Test
	public void differentDecision_fails() {
		expect("{ \"decision\": true }", "{ \"decision\": false }");
		fails(new EnsureAuthzenDecisionResponseValMatchesExpected());
	}

	@Test
	public void stringDecision_fails() {
		expect("{ \"decision\": true }", "{ \"decision\": \"true\" }");
		fails(new EnsureAuthzenDecisionResponseValMatchesExpected());
	}

	@Test
	public void missingDecision_fails() {
		expect("{ \"decision\": true }", "{ \"evaluations\": [] }");
		fails(new EnsureAuthzenDecisionResponseValMatchesExpected());
	}

	@Test
	public void expectationWithoutDecision_isRejected() {
		fails(new ExtractAuthzenDecisionExpectedResponse("{ \"evaluations\": [] }"));
	}
}
