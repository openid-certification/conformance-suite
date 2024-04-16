package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.ExpectRequestObjectMissingStateErrorPage;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.VerifyNoStateInAuthorizationResponse;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractFAPI2SPID2EnsureRequestObjectWithoutState extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingStateErrorPage.class, "RFC6749-4.1.1");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.skip(AddStateToAuthorizationEndpointRequest.class,
						"NOT adding state to request object");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)
		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			callAndStopOnFailure(CheckMatchingCallbackParameters.class);

			callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

			callAndContinueOnFailure(VerifyNoStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

			callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

			callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

			handleSuccessfulAuthorizationEndpointResponse();

		} else {
			/* If we get an error back from the authorization server:
			 * - It must be a 'invalid_request_object', 'invalid_request' or 'access_denied' error
			 * - It must have the correct state we supplied
			 */

			// state can be absented if authorization request did not send state in the request object
			skipIfElementMissing("authorization_endpoint_response",  "state", Condition.ConditionResult.INFO,
				CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");

			fireTestFinished();
		}
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}
}
