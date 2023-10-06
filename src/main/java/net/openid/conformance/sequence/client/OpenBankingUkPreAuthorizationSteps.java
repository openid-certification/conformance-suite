package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIFinancialIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallAccountRequestsEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckIfAccountRequestsEndpointResponseError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateCreateAccountRequestRequest;
import net.openid.conformance.condition.client.CreateCreateAccountRequestRequestWithExpiration;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
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
	private boolean includeXFapiFinancialId;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingUkPreAuthorizationSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this(secondClient,
			true, // for FAPIID2 tests
			addClientAuthenticationToTokenEndpointRequest);
	}

	public OpenBankingUkPreAuthorizationSteps(boolean secondClient, boolean includeXFapiFinancialId, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.secondClient = secondClient;
		this.currentClient = secondClient ? "Second client: " : "";
		this.includeXFapiFinancialId = includeXFapiFinancialId;
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

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.WARNING, "RFC6749-4.4.3", "RFC6749-5.1");

		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC6749-5.1")
				.onFail(Condition.ConditionResult.FAILURE)
				.dontStopOnFailure());

		/* create account request */

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

		if (includeXFapiFinancialId) {
			// This header is no longer mentioned in the FAPI standard as of ID2, however the UK OB spec most banks are
			// using (v3.1.1) erroneously requires that this header is sent in all cases. We send it in the ID2 tests,
			// but not in FAPI1-Final
			callAndStopOnFailure(AddFAPIFinancialIdToResourceEndpointRequest.class);
		}

		if (secondClient) {
			callAndStopOnFailure(CreateCreateAccountRequestRequestWithExpiration.class);
		} else {
			callAndStopOnFailure(CreateCreateAccountRequestRequest.class);
		}

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);

		call(exec().endBlock());
	}
}
