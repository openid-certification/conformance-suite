package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError;
import net.openid.conformance.condition.client.ExpectInvalidRequestInvalidRequestObjectOrAccessDeniedErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-server-handles-non-matching-intent-id",
	displayName = "FAPI2-Security-Profile-ID2: server handles non matching openbanking_intent_id",
	summary = "This test registers an intent/consent with one client and then uses it with a different client. It should end with the user being redirected back to the conformance suite (with an invalid_request, invalid_request_object or access_denied error), or with the authorization server displaying an error message to the user saying the intent/consent id belongs to a different client (a screenshot of which must be uploaded).",
	profile = "FAPI2-Security-Profile-ID2",
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
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2ID2OPProfile.class, values = { "plain_fapi", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae" })
public class FAPI2SPID2EnsureServerHandlesNonMatchingIntentId extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		// client2 is used for the Authorization Request so make sure we use the redirect url normally used with client2
		callAndContinueOnFailure(AddRedirectUriQuerySuffix.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.2");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	@Override
	protected void createAuthorizationRequest() {
		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Client2, Jwks2, tls2");
		switchToSecondClient();
		super.createAuthorizationRequest();
	}

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		unmapClient();
		eventLog.endBlock();
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
		callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		fireTestFinished();

	}
}
