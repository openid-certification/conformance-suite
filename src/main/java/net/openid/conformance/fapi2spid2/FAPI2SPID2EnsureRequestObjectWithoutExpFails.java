package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestUriOrAccessDeniedError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRequestObjectMissingExpClaimErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-request-object-without-exp-fails",
	displayName = "FAPI2-Security-Profile-ID2: ensure request object without exp fails",
	summary = "This test should end with the authorization server showing an error message: invalid_request, invalid_request_object (for request object by value), invalid_request_uri (when PAR in use) or access_denied (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
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
@VariantNotApplicable(parameter = FAPI2AuthRequestMethod.class, values = { "unsigned" })
public class FAPI2SPID2EnsureRequestObjectWithoutExpFails extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingExpClaimErrorPage.class, "FAPI2-MS-ID1-5.3.1-4");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
				.skip(AddExpToRequestObject.class, "NOT adding exp to request object");
	}

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "JAR-6.2","PAR-2.3");

		fireTestFinished();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorization server:
		 * - It must be a 'invalid_request_object', 'invalid_request' or 'access_denied' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		if (isPar) {
			callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestUriOrAccessDeniedError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		} else {
			callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		}
		fireTestFinished();

	}
}
