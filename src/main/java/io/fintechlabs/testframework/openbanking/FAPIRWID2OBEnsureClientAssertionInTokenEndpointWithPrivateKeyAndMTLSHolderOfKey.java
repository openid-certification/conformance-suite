package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallAccountRequestsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequest;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequestWithExpiration;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetAccountScopeOnTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.fapi.FAPIRWID2;
import io.fintechlabs.testframework.fapi.FAPIRWID2EnsureClientAssertionInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ob-ensure-client-assertion-in-token-endpoint-with-private-key-and-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2-OB: ensure client_assertion in token endpoint (private key authentication and mtls holder of key)",
	summary = "This test should end with the token endpoint returning an error message that was 'invalid_client' or 'invalid_request'.",
	profile = "FAPI-RW-ID2-OB",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"resource.institution_id"
	},
	notApplicableForVariants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_privatekeyjwt,
		FAPIRWID2.variant_openbankinguk_mtls
	}
)
public class FAPIRWID2OBEnsureClientAssertionInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey extends FAPIRWID2EnsureClientAssertionInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey {

	@Variant(name = variant_openbankinguk_privatekeyjwt)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		super.performAuthorizationFlow();
	}

	protected void performPreAuthorizationSteps() {
		eventLog.startBlock(currentClientString() + "Use client_credentials grant to obtain OpenBanking UK intent_id");

		/* get an openbanking intent id */
		requestClientCredentialsGrant();

		createAccountRequest();

		eventLog.endBlock();
	}

	@Override
	protected void performProfileIdTokenValidation() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest.class);
	}

	protected void createClientCredentialsRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetAccountScopeOnTokenEndpointRequest.class);

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}

	protected void requestClientCredentialsGrant() {

		createClientCredentialsRequest();

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
	}

	protected void createAccountRequest() {

		if ( whichClient == 2 ) {
			callAndStopOnFailure(CreateCreateAccountRequestRequestWithExpiration.class);

		} else {
			callAndStopOnFailure(CreateCreateAccountRequestRequest.class);
		}

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);
	}
}
