package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddExpValueIs70MinutesInFutureToRequestObject;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestObjectError;
import net.openid.conformance.condition.client.EnsureInvalidRequestUriError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRequestObjectWithExpOver60ClaimErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-request-object-with-exp-over-60-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure request object with exp value of more then than 60 minutes after the nbf value fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
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
@VariantNotApplicable(parameter = FAPI2AuthRequestMethod.class, values = { "unsigned" })
public class FAPI2SPFinalEnsureRequestObjectWithExpOver60Fails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectWithExpOver60ClaimErrorPage.class, "FAPI2-MS-ID1-5.3.1-4");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
				.replace(AddExpToRequestObject.class,
						condition(AddExpValueIs70MinutesInFutureToRequestObject.class).requirements("OIDCC-6.1", "RFC7519-4.1.4"));
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "JAR-6.3", "PAR-2.1-3");
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
		if (isPar) {
			callAndContinueOnFailure(EnsureInvalidRequestUriError.class, Condition.ConditionResult.FAILURE, "JAR-4");
		} else {
			callAndContinueOnFailure(EnsureInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		}
		fireTestFinished();

	}
}
