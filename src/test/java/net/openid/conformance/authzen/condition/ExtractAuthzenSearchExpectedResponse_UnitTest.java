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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ExtractAuthzenSearchExpectedResponse_UnitTest {

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
	public void searchExpectation_isStored() {
		run(new ExtractAuthzenSearchExpectedResponse("{ \"results\": [ { \"name\": \"read\" } ] }"));
		assertEquals(1, env.getObject("authzen_search_endpoint_expected_response").getAsJsonArray("results").size());
	}

	@Test
	public void searchExpectationWithoutResults_isRejected() {
		fails(new ExtractAuthzenSearchExpectedResponse("{ \"page\": {} }"));
	}

	@Test
	public void searchExpectationWithNonArrayResults_isRejected() {
		fails(new ExtractAuthzenSearchExpectedResponse("{ \"results\": {} }"));
	}
}
