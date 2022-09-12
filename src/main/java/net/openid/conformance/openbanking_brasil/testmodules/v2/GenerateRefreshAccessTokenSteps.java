package net.openid.conformance.openbanking_brasil.testmodules.v2;

import net.openid.conformance.condition.Condition;
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
		call(condition(ExtractRefreshTokenFromTokenResponse.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.INFO));

		callAndStopOnFailure(GenerateRefreshTokenRequest.class);

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
			callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);
			callAndStopOnFailure(SignClientAuthenticationAssertion.class);
			callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
		}

		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
	}
}
