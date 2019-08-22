package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddScopeToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.CreateRefreshTokenRequest;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;

public class RefreshTokenRequestExpectingErrorSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public RefreshTokenRequestExpectingErrorSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.secondClient = secondClient;
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		if (!secondClient) {
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");
		}

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
