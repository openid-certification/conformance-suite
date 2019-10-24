package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.ExpectAuthorizationRequestWithoutRequestObjectErrorPage;
import net.openid.conformance.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-authorization-request-without-request-object-fails",
	displayName = "FAPI-RW-ID2: ensure authorization request without request_object fails",
	summary = "This test should end with the authorisation server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
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
	}
)
public class FAPIRWID2EnsureAuthorizationRequestWithoutRequestObjectFails extends AbstractFAPIRWID2ExpectingAuthorizationFailure {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectAuthorizationRequestWithoutRequestObjectErrorPage.class, "FAPI-RW-5.2.2-1");

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRedirect() {

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class, "FAPI-RW-5.2.2-1");
	}


	@Override
	protected void onAuthorizationCallbackResponse() {
		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request' error
		 * - It must have the correct state we supplied
		 */

		// state can be absented if authorization request did not send state in the request object
		skipIfElementMissing("authorization_endpoint_response",  "state", Condition.ConditionResult.INFO,
			CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
		fireTestFinished();
	}
}
