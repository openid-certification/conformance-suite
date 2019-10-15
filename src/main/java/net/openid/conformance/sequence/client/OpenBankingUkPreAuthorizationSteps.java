package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallAccountRequestsEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckIfAccountRequestsEndpointResponseError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateCreateAccountRequestRequest;
import net.openid.conformance.condition.client.CreateCreateAccountRequestRequestWithExpiration;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.SetAccountScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class OpenBankingUkPreAuthorizationSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingUkPreAuthorizationSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.secondClient = secondClient;
		this.currentClient = secondClient ? "Second client: " : "";
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {
		call(exec().startBlock(currentClient + "Use client_credentials grant to obtain OpenBanking UK intent_id"));

		/* create client credentials request */

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		/* get an openbanking intent id */

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);

		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC6749-5.1")
				.onFail(Condition.ConditionResult.FAILURE)
				.dontStopOnFailure());

		/* create account request */

		if (secondClient) {
			callAndStopOnFailure(CreateCreateAccountRequestRequestWithExpiration.class);
		} else {
			callAndStopOnFailure(CreateCreateAccountRequestRequest.class);
		}

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);

		call(exec().endBlock());
	}
}
