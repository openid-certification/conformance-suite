package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExpectRedirectUriMissingErrorPage;
import net.openid.conformance.condition.client.RemoveRedirectUriFromAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-redirect_uri-Missing
@PublishTestModule(
	testName = "oidcc-ensure-redirect-uri-in-authorization-request",
	displayName = "OIDCC: ensure redirect URI in authorization request",
	summary = "This test should result an the authorization server showing an error page saying the redirect url is missing from the request (a screenshot of which should be uploaded)",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client2.scope",
		"resource.resourceUrl"
	}
)
public class OIDCCEnsureRedirectUriInAuthorizationRequest extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "OIDCC-3.1.2.1");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_missing_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(RemoveRedirectUriFromAuthorizationEndpointRequest.class)));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request_object' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();
	}
}
