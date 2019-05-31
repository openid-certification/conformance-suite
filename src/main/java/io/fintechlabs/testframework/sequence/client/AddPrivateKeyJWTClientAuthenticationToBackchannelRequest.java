package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SetDynamicRegistrationRequestGrantTypeToAuthorizationCode;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.condition.client.UnregisterDynamicallyRegisteredClient;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddPrivateKeyJWTClientAuthenticationToBackchannelRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		// The spec isn't 100% clear on whether this is the correct thing to do; see:
		// https://bitbucket.org/openid/mobile/issues/155/aud-to-use-in-client_assertion-passed-to
		callAndStopOnFailure(SetClientAuthenticationAudToBackchannelAuthenticationEndpoint.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToBackchannelAuthenticationEndpoint.class);

	}

}
