package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.ExpectAuthorizationRequestWithoutRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-unsigned-authorization-request-without-using-par-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure unsigned authorization request without using PAR fails",
	summary = "This test calls the authorization endpoint without using a request object (i.e. with all the parameters passed in the url query), and should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with an invalid_request error.",
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

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })

public class FAPI2SPFinalEnsureUnsignedAuthorizationRequestWithoutUsingParFails extends AbstractFAPI2SPFinalExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectAuthorizationRequestWithoutRequestObjectErrorPage.class, "FAPI2-MS-ID1-5.3.1-1");

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRequestObject() {
		// Nothing as no request object required in this test
	}

	@Override
	protected void buildRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class, "FAPI2-MS-ID1-5.3.1-1");
	}

	@Override
	protected void performAuthorizationFlow() {
		isPar = false; // we're passing a non-request object to the authorization endpoint, so we never want to call par endpoint
		super.performAuthorizationFlow();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		/* If we get an error back from the authorization server:
		 * - It must be a 'invalid_request' error
		 * - It must have the correct state we supplied
		 */

		// state can be absented if authorization request did not send state in the request object
		skipIfElementMissing("authorization_endpoint_response",  "state", Condition.ConditionResult.INFO,
			CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
		fireTestFinished();
	}
}
