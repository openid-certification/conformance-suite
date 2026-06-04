package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallAuthzenApiEndpointAndVerifyExpectedStatus extends AbstractConditionSequence {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request"}, strings = "authzen_api_endpoint")
	public void evaluate() {
		callAndStopOnFailure(CallAuthzenApiEndpointAllowingJsonParseFailure.class, "AUTHZEN-10");

		call(exec().mapKey("endpoint_response", "authzen_api_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeMatchesExpected.class, Condition.ConditionResult.FAILURE, "AUTHZEN-10.1");
		call(exec().unmapKey("endpoint_response"));
	}
}
