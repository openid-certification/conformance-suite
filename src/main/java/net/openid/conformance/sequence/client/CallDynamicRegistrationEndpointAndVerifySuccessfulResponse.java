package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckNoErrorFromDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.ExtractDynamicRegistrationResponse;
import net.openid.conformance.condition.client.VerifyClientManagementCredentials;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallDynamicRegistrationEndpointAndVerifySuccessfulResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE,"OIDCR-3.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE,"OIDCR-3.2");
		callAndContinueOnFailure(CheckNoErrorFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
		callAndStopOnFailure(ExtractDynamicRegistrationResponse.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
		callAndContinueOnFailure(VerifyClientManagementCredentials.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");

		call(exec().unmapKey("endpoint_response"));
	}
}
