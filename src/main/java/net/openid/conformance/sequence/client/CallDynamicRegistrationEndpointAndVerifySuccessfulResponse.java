package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.ExtractDynamicRegistrationResponse;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallDynamicRegistrationEndpointAndVerifySuccessfulResponse extends AbstractConditionSequence {

	public CallDynamicRegistrationEndpointAndVerifySuccessfulResponse() {
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE);

		call(exec().unmapKey("endpoint_response"));
	}
}
