package net.openid.conformance.authzen.condition;

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

@ExtendWith(MockitoExtension.class)
class EnsureBackwardCompatResponseHasNoEvaluationsArray_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureBackwardCompatResponseHasNoEvaluationsArray cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureBackwardCompatResponseHasNoEvaluationsArray();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		env.putObject("authzen_api_endpoint_decision", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void singleDecision_succeeds() {
		putResponse("{ \"decision\": true }");
		cond.execute(env);
	}

	@Test
	public void decisionWithExtraContext_succeeds() {
		putResponse("{ \"decision\": true, \"context\": { \"reason\": \"ok\" } }");
		cond.execute(env);
	}

	@Test
	public void evaluationsArrayPresent_fails() {
		putResponse("{ \"evaluations\": [ { \"decision\": true } ] }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void bothDecisionAndEvaluations_fails() {
		putResponse("{ \"decision\": true, \"evaluations\": [ { \"decision\": true } ] }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
