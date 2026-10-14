package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ExtractAuthzenEvaluationsExpectedResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	@Test
	public void evaluationsExpectation_isStored() {
		run(new ExtractAuthzenEvaluationsExpectedResponse("{ \"evaluations\": [ { \"decision\": true } ] }"));
		assertFalse(env.getObject("authzen_evaluations_endpoint_expected_response").getAsJsonArray("evaluations").isEmpty());
	}

	@Test
	public void evaluationsExpectationWithoutEvaluations_isRejected() {
		fails(new ExtractAuthzenEvaluationsExpectedResponse("{ \"decision\": true }"));
	}

	@Test
	public void evaluationsExpectationWithNonArrayEvaluations_isRejected() {
		fails(new ExtractAuthzenEvaluationsExpectedResponse("{ \"evaluations\": true }"));
	}
}
