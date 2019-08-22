package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.EnsureAccessDeniedErrorFromAuthorizationEndpointResponse;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorPage;
import io.fintechlabs.testframework.condition.client.OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-server-not-allow-acr-claim",
	displayName = "FAPI-RW-ID2: server returns error if it doesn't allow sca acr claim",
	summary = "This test is verifying the server allows sca acr claim or not, if not the server will show an access_denied error (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response, If server allows then flow will complete normally",
	profile = "FAPI-RW-ID2",
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
		FAPIRWID2.variant_privatekeyjwt
	}
)
public class FAPIRWID2ServerNotAllowScaAcrClaim extends AbstractFAPIRWID2ServerTestModule {

	@Variant(
		name = variant_openbankinguk_mtls,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
		profileAuthorizationEndpointSetupSteps = OpenBankingUkAuthorizationEndpointOverridingSetup.class;
	}

	@Variant(
		name = variant_openbankinguk_privatekeyjwt,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();

		profileAuthorizationEndpointSetupSteps = OpenBankingUkAuthorizationEndpointOverridingSetup.class;
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectAccessDeniedErrorPage.class);

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");
		if (callbackParams.has("error")) {
			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(EnsureAccessDeniedErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE);

			fireTestFinished();
		} else {
			super.onAuthorizationCallbackResponse();
		}
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		checkAccountRequestEndpointTLS();

		checkAccountResourceEndpointTLS();

		requestProtectedResource();

		verifyAccessTokenWithResourceEndpoint();

		fireTestFinished();
	}

	public static class OpenBankingUkAuthorizationEndpointOverridingSetup extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest.class);
		}
	}
}
