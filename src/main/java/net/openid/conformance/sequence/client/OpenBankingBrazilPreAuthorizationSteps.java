package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractConsentIdFromConsentEndpointResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.FAPIBrazilAddConsentIdToClientScope;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilConsentEndpointResponseValidatePermissions;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class OpenBankingBrazilPreAuthorizationSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingBrazilPreAuthorizationSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.secondClient = secondClient;
		this.currentClient = secondClient ? "Second client: " : "";
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

		@Override
	public void evaluate() {
		call(exec().startBlock(currentClient + "Use client_credentials grant to obtain Brazil consent"));

		/* create client credentials request */

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		/* get access token */

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, "RFC6749-4.4.3", "RFC6749-5.1");

		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC6749-5.1")
				.onFail(Condition.ConditionResult.FAILURE)
				.dontStopOnFailure());

		/* create consent request */

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);

		callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);

		callAndStopOnFailure(CallConsentEndpointWithBearerToken.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);

		callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

		call(exec().endBlock());
	}
}
