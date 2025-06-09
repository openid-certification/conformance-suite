package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectRequestObjectWithLongNonceErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-request-object-with-64-char-nonce-success",
	displayName = "FAPI1-Advanced-Final: ensure request object with 64 char nonce",
	summary = "This test passes a 64 character nonce in the request object. As per https://bitbucket.org/openid/fapi/pull-requests/476/diff the authorization server must either successfully authenticate and return the nonce correctly, or if it does not support nonces that long it may return an invalid_request error back to the client or show an error page (saying the server rejects the nonce - upload a screenshot of the error page). The server MUST NOT return the nonce corrupted or truncated.",
	profile = "FAPI1-Advanced-Final",
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
public class FAPI1AdvancedFinalEnsureRequestObjectWith64CharNonceSuccess extends AbstractFAPI1AdvancedFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectRequestObjectWithLongNonceErrorPage.class, Condition.ConditionResult.WARNING);
		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		Command cmd = new Command();
		cmd.putInteger("requested_nonce_length", 64);
		return super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			super.onAuthorizationCallbackResponse();

		} else {

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.WARNING, "OIDCC-3.3.2.6");

			fireTestFinished();
		}
	}
}
