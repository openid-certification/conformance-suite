package net.openid.conformance.openbanking_brasil.testmodules.v2;

import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.variant.ClientAuthType;

public class GenerateRefreshAccessTokenSteps extends AbstractConditionSequence {
	private ClientAuthType clientAuthType;

	public GenerateRefreshAccessTokenSteps(ClientAuthType clientAuthType) {
		this.clientAuthType = clientAuthType;
	}
	@Override
	public void evaluate() {
		call(exec().startBlock("Refreshing Access Token"));
		callAndStopOnFailure(GenerateRefreshTokenRequest.class);

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
			callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);
			callAndStopOnFailure(SignClientAuthenticationAssertion.class);
			callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
		} else {
			skip(CreateClientAuthenticationAssertionClaims.class, "Skipping step for MTLS");
			skip(SignClientAuthenticationAssertion.class, "Skipping step for MTLS");
			skip(AddClientAssertionToTokenEndpointRequest.class, "Skipping step for MTLS");
			callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class, "Skipping step for MTLS");
		}

		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
	}
}
