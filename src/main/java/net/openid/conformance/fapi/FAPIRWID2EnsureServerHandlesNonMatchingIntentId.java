package net.openid.conformance.fapi;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError;
import net.openid.conformance.condition.client.ExpectInvalidRequestInvalidRequestObjectOrAccessDeniedErrorPage;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-server-handles-non-matching-intent-id",
	displayName = "FAPI-RW-ID2: server handles non matching openbanking_intent_id",
	summary = "This test registers an intentId with one client and then uses it with a different client. It should end with the user being redirected back to the conformance suite with a correct error response.",
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
		"client2.jwks",
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
@VariantNotApplicable(parameter = FAPIProfile.class, values = { "plain_fapi" })
public class FAPIRWID2EnsureServerHandlesNonMatchingIntentId extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		// client2 is used for the Authorization Request so make sure we use the redirect url normally used with client2
		callAndContinueOnFailure(AddRedirectUriQuerySuffix.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.2");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	@Override
	protected void performAuthorizationFlow() {

		performPreAuthorizationSteps();

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Client2, Jwks2, tls2");
		switchToSecondClient();

		createAuthorizationRequest();
		createAuthorizationRedirect();

		unmapClient();

		eventLog.endBlock();

		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestInvalidRequestObjectOrAccessDeniedErrorPage.class);

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		fireTestFinished();

	}
}
