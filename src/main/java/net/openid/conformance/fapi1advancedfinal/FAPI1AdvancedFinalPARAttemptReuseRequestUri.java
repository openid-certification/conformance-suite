package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestUriError;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-7.3 : An attacker could replay a request URI captured from a legitimate authorization request.
// In order to cope with such attacks, the AS SHOULD make the request URIs one-time use.
@PublishTestModule(
	testName = "fapi1-advanced-final-par-attempt-reuse-request_uri",
	displayName = "PAR : try to reuse a request_uri ",
	summary = "This test tries to use a request_uri twice and expects the authorization server either show an error or redirect back with an invalid_request_uri error, PAR section 7.3 states 'the AS SHOULD make the request URIs one-time use'. If the authentication succeeds a warning will be issued.",
	profile = "FAPI1-Advanced-Final",
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
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
public class FAPI1AdvancedFinalPARAttemptReuseRequestUri extends AbstractFAPI1AdvancedFinalServerTestModule {
	boolean secondAttempt = false;

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-7.3", "PAR-4", "PAR-2.2");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));

		eventLog.endBlock();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (secondAttempt) {
			// the server does not implement the 'should'. When we get here we have verified the successful response
			// when reusing the request_uri and have already logged a warning, so the test can now end
			fireTestFinished();
			return;
		}
		eventLog.startBlock("Attempting reuse of request_uri and testing if Authorization server returns error in callback");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class);
		secondAttempt = true;
		allowPlainErrorResponseForJarm = true;

		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		if (!secondAttempt) {
			// first authentication is a normal successful one
			super.onAuthorizationCallbackResponse();
			return;
		}

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (callbackParams.has("error")) {
			callAndContinueOnFailure(EnsureInvalidRequestUriError.class, Condition.ConditionResult.FAILURE, "PAR-2.2", "JAR-7");

			eventLog.endBlock();

			fireTestFinished();
		} else {
			// second authentication "should" return an error, but only a should so warn but otherwise expect a successful response
			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.WARNING, "PAR-7.3");

			super.onAuthorizationCallbackResponse();
		}
	}
}
