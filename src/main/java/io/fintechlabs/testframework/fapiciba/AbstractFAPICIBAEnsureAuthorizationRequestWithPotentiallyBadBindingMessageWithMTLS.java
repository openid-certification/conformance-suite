package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddPotentiallyBadBindingMessageToAuthorizationEndpointRequestResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage;
import io.fintechlabs.testframework.condition.client.ExpectBindingMessageCorrectDisplay;

public abstract class AbstractFAPICIBAEnsureAuthorizationRequestWithPotentiallyBadBindingMessageWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(AddPotentiallyBadBindingMessageToAuthorizationEndpointRequestResponse.class, "CIBA-7.1");

	}

	@Override
	protected void performValidateAuthorizationResponse() {

		JsonObject callbackParams = env.getObject("backchannel_authentication_endpoint_response");

		if (callbackParams != null && callbackParams.has("error")) {

			validateErrorFromBackchannelAuthorizationRequestResponse();

			callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage.class, Condition.ConditionResult.FAILURE, "CIBA-13");

			fireTestFinished();

		} else {
			super.performValidateAuthorizationResponse();
		}
	}

	@Override
	protected void performPostAuthorizationFlow() {

		verifyAccessTokenWithProtectedResource();

		setStatus(Status.WAITING);

		// ask the user to upload a screenshot/photo of the binding message being correctly displayed when the server authenticates successfully
		callAndContinueOnFailure(ExpectBindingMessageCorrectDisplay.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");

		waitForPlaceholders();

	}
}
