package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddClientAssertionToBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.SetClientAuthenticationAudIssuerIdentifierToBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.sequence.AbstractConditionSequence;

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
				callAndStopOnFailure(SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint.class, "CIBA-7.1");
			} else {
				// value of the audience is Issuer Identifier URL
				callAndStopOnFailure(SetClientAuthenticationAudIssuerIdentifierToBackchannelAuthenticationEndpoint.class, "CIBA-7.1");
			}
		}

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToBackchannelAuthenticationEndpoint.class);

	}

}
