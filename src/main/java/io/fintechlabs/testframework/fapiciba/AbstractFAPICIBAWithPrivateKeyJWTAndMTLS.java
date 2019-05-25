package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;

public abstract class AbstractFAPICIBAWithPrivateKeyJWTAndMTLS extends AbstractFAPICIBA {

	@Override
	protected void addClientAuthenticationToBackchannelRequest(){

		callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		// The spec isn't 100% clear on whether this is the correct thing to do; see:
		// https://bitbucket.org/openid/mobile/issues/155/aud-to-use-in-client_assertion-passed-to
		callAndStopOnFailure(SetClientAuthenticationAudToBackchannelAuthenticationEndpoint.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToBackchannelAuthenticationEndpoint.class);

	}

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest(){

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);

	}

}
