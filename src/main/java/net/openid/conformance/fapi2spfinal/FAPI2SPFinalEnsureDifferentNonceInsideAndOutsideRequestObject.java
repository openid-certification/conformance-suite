package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIncorrectNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.ExpectRequestDifferentNonceInsideAndOutsideErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-different-nonce-inside-and-outside-request-object",
	displayName = "FAPI2-Security-Profile-Final: ensure different nonce inside and outside request object",
	summary = "This test passes a different nonce in the authorization_endpoint parameters to the one inside the signed request object. There are 3 valid ways the authorization server may handle this: it must either return an invalid_request error back to the client, or must show an error page (saying the request is invalid as the 'nonce' value in the request object and outside it are different - upload a screenshot of the error page), or must successfully authenticate and return the nonce from inside the request object in the id_token.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = { "plain_oauth" })
public class FAPI2SPFinalEnsureDifferentNonceInsideAndOutsideRequestObject extends AbstractFAPI2SPFinalExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestDifferentNonceInsideAndOutsideErrorPage.class, "OIDCC-6.1");

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected void performPARRedirectWithRequestUri() {
		// Note: BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint and
		// BuildRequestObjectByValueRedirectToAuthorizationEndpoint include as URL
		// parameters values in "authorization_endpoint_request" which differ or are
		// missing from the request object. Here, an incorrect nonce is added as a parameter.
		callAndStopOnFailure(AddIncorrectNonceToAuthorizationEndpointRequest.class);

		super.performPARRedirectWithRequestUri();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)
		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			super.onAuthorizationCallbackResponse();

		} else {
			/* If we get an error back from the authorization server:
			 * - It must be a 'invalid_request' error
			 * - It must have the correct state we supplied
			 */

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
			fireTestFinished();
		}
	}
}
