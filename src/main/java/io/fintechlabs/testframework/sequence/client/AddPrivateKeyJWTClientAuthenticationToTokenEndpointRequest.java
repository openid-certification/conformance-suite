package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}

}
