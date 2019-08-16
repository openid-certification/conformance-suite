package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.AddClientAssertionToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudIssuerIdentifierToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddPrivateKeyJWTClientAuthenticationToBackchannelRequest extends AbstractConditionSequence {

	private boolean isSecondClient;

	private boolean isDefaultClientAssertionAud;

	public AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(boolean isSecondClient, boolean isDefaultClientAssertionAud) {
		this.isSecondClient = isSecondClient;
		this.isDefaultClientAssertionAud = isDefaultClientAssertionAud;
	}

	@Override
	public void evaluate() {

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		if (isDefaultClientAssertionAud) {
			// value of the audience is backchannel authentication endpoint URL
			callAndStopOnFailure(SetClientAuthenticationAudToBackchannelAuthenticationEndpoint.class);
		} else {
			if (this.isSecondClient) {
				// value of the audience is token endpoint URL
				callAndStopOnFailure(SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint.class);
			} else {
				// value of the audience is Issuer Identifier URL
				callAndStopOnFailure(SetClientAuthenticationAudIssuerIdentifierToBackchannelAuthenticationEndpoint.class);
			}
		}

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToBackchannelAuthenticationEndpoint.class);

	}

}
