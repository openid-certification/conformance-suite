package net.openid.conformance.fapi2spfinal;

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
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-request-object-with-long-nonce",
	displayName = "FAPI2-Security-Profile-Final: ensure request object with long nonce",
	summary = "This test passes a 384 character nonce in the request object. The authorization server must either successfully authenticate and return the nonce correctly, or if it does not support nonces that long it may return an invalid_request error back to the client or show an error page (saying the server rejects long nonce - upload a screenshot of the error page). The server MUST NOT return the nonce corrupted or truncated.",
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
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = { "plain_oauth" })
public class FAPI2SPFinalEnsureRequestObjectWithLongNonce extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectRequestObjectWithLongNonceErrorPage.class, Condition.ConditionResult.WARNING);
		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		// Add long nonce with 384 bytes
		// see https://gitlab.com/openid/conformance-suite/-/issues/359 for background
		Command cmd = new Command();
		cmd.putInteger("requested_nonce_length", 384);
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
