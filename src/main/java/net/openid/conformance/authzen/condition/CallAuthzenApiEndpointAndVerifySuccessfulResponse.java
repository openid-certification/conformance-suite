package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallAuthzenApiEndpointAndVerifySuccessfulResponse extends AbstractConditionSequence {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_response"})
	public void evaluate() {
		callAndStopOnFailure(CallAuthzenApiEndpoint.class, "AUTHZEN-10");

		call(exec().mapKey("endpoint_response", "authzen_api_endpoint_response"));

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE,"AUTHZEN-10.1");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE,"AUTHZEN-10.1");
		call(exec().unmapKey("endpoint_response"));
	}
}
