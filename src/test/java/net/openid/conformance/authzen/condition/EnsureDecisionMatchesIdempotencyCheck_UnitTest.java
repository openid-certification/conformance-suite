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

@ExtendWith(MockitoExtension.class)
class EnsureDecisionMatchesIdempotencyCheck_UnitTest {

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

	@Test
	public void match_sameDecision_succeeds() {
		env.putString("authzen_idempotency_first_decision", "false");
		env.putObject("authzen_api_endpoint_decision", json("{ \"decision\": false }"));
		run(new EnsureDecisionMatchesIdempotencyCheck());
	}

	@Test
	public void match_changedDecision_fails() {
		env.putString("authzen_idempotency_first_decision", "true");
		env.putObject("authzen_api_endpoint_decision", json("{ \"decision\": false }"));
		fails(new EnsureDecisionMatchesIdempotencyCheck());
	}

	@Test
	public void match_nonBooleanDecision_fails() {
		env.putString("authzen_idempotency_first_decision", "true");
		env.putObject("authzen_api_endpoint_decision", json("{ \"decision\": 1 }"));
		fails(new EnsureDecisionMatchesIdempotencyCheck());
	}
}
