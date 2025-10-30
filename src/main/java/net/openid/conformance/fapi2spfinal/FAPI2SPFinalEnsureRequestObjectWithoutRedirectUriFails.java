package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRequestObjectMissingRedirectUriErrorPage;
import net.openid.conformance.condition.client.RemoveRedirectUriFromRequestObject;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-request-object-without-redirect-uri-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure request object without redirect_uri fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid due to the missing redirect uri (a screenshot of which should be uploaded), or in an error from the PAR endpoint, or (in the case where the client only has one redirect_uri registered) with the user being redirected back to the client's registered redirect_uri with a correct error response.",
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
public class FAPI2SPFinalEnsureRequestObjectWithoutRedirectUriFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingRedirectUriErrorPage.class, "FAPI2-MS-ID1-5.3.2-1");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
				.insertBefore(SignRequestObject.class,
						condition(RemoveRedirectUriFromRequestObject.class));
	}

	@Override
	protected void performParAuthorizationRequestFlow() {
		callAndStopOnFailure(RemoveRedirectUriFromRequestObject.class);
		super.performParAuthorizationRequestFlow();
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "JAR-6.2");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorization server:
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
