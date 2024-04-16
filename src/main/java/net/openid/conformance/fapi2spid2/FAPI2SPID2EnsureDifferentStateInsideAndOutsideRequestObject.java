package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIncorrectStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.ExpectRequestDifferentStateInsideAndOutsideErrorPage;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-different-state-inside-and-outside-request-object",
	displayName = "FAPI2-Security-Profile-ID2: ensure different state inside and outside request object",
	summary = "This test passes a different state in the authorization_endpoint parameters to the one inside the signed request object. There are 3 valid ways the authorization server may handle this: it must either return an invalid_request error back to the client, or must show an error page (saying the request is invalid as the 'state' value in the request object and outside it are different - upload a screenshot of the error page), or must successfully authenticate and return the state from inside the request object in the id_token.",
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
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)

public class FAPI2SPID2EnsureDifferentStateInsideAndOutsideRequestObject extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestDifferentStateInsideAndOutsideErrorPage.class, "FAPI2-MS-ID1-5.3.2-1");

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		// Note: BuildRequestObjectByValueRedirectToAuthorizationEndpoint includes
		// as URL parameters values in "authorization_endpoint_request"
		// which differ or are missing from the request object.
		// Here, an incorrect nonce is added as a parameter.
		return super.makeCreateAuthorizationRequestObjectSteps()
				.insertAfter(SignRequestObject.class,
						condition(AddIncorrectStateToAuthorizationEndpointRequest.class));
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
