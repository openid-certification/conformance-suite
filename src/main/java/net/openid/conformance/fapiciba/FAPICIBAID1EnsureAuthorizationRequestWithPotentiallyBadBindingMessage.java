package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage;
import net.openid.conformance.condition.client.ExpectBindingMessageCorrectDisplay;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-potentially-bad-binding-message",
	displayName = "FAPI-CIBA-ID1: Test with a potentially bad binding message, the server should authenticate successfully or return the invalid_binding_message error",
	summary = "This test tries sending a potentially bad binding message to authorization endpoint request - i.e. a message that the Authentication Device may not be able to display, as it is quite long and contains various non-ASCII characters. The server should either:\n\n * showing the correct binding message (a screenshot/photo of which should be uploaded) and the users successfully authenticates\n\n * or, if it is unable to display the message, return the invalid_binding_message error.\n\nThe binding message used is:\n\n" + AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest.POTENTIALLY_BAD_BINDING_MESSAGE,
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAID1EnsureAuthorizationRequestWithPotentiallyBadBindingMessage extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest.class, "CIBA-7.1");

	}

	@Override
	protected void performAuthorizationFlow() {

		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		JsonObject callbackParams = env.getObject("backchannel_authentication_endpoint_response");

		if (callbackParams != null && callbackParams.has("error")) {

			validateErrorFromBackchannelAuthorizationRequestResponse();

			callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage.class, Condition.ConditionResult.FAILURE, "CIBA-13");

			fireTestFinished();

		} else {

			performValidateAuthorizationResponse();

			eventLog.endBlock();

			performPostAuthorizationResponse();

		}
	}

	@Override
	protected void performPostAuthorizationFlow(boolean finishTest) {

		requestProtectedResource();

		// ask the user to upload a screenshot/photo of the binding message being correctly displayed when the server authenticates successfully
		callAndContinueOnFailure(ExpectBindingMessageCorrectDisplay.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");

		setStatus(Status.WAITING);

		waitForPlaceholders();
	}
}
