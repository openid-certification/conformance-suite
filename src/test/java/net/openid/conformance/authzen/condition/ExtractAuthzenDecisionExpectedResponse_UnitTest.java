package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractAuthzenDecisionExpectedResponse_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractAuthzenDecisionExpectedResponse cond(String expected) {
		ExtractAuthzenDecisionExpectedResponse cond = new ExtractAuthzenDecisionExpectedResponse(expected);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return cond;
	}

	@Test
	public void expectedDecision_isStored() {
		cond("{ \"decision\": true }").execute(env);
		assertTrue(OIDFJSON.getBoolean(env.getObject("authzen_decision_endpoint_expected_response").get("decision")));
	}

	@Test
	public void expectedResponseWithoutDecision_fails() {
		ExtractAuthzenDecisionExpectedResponse cond = cond("{ \"context\": {} }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
