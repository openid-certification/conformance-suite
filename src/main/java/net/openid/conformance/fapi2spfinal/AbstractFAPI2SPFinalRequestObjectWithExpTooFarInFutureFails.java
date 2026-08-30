package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddExpValueIs15MinutesInFutureToRequestObject;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestObjectError;
import net.openid.conformance.condition.client.EnsureInvalidRequestUriError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRequestObjectWithExpOver60ClaimErrorPage;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Sends a request object whose exp is 15 minutes in the future and expects the authorization
 * server to reject it. Ecosystems that cap the request object lifetime at 10 minutes subclass
 * this and supply their own requirement references.
 */
public abstract class AbstractFAPI2SPFinalRequestObjectWithExpTooFarInFutureFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	/**
	 * @return the requirement references for the profile's request object exp limit.
	 */
	protected abstract String[] getExpRequirements();

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectWithExpOver60ClaimErrorPage.class, "FAPI2-MS-ID1-5.3.1-4");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
				.replace(AddExpToRequestObject.class,
						condition(AddExpValueIs15MinutesInFutureToRequestObject.class).requirements(getExpRequirements()));
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
