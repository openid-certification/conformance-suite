package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidRequestUri;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectAuthorizationRequestWithoutRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-unsigned-request-at-par-endpoint-fails",
	displayName = "FAPI2-Security-Profile-ID2: ensure unsigned request at PAR endpoint fails",
	summary = "This test passes the request parameters to the PAR endpoint without them being wrapped in a signed request object, which should be rejected as signed request objects are required.\n\nThe test should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded), or in an error from the PAR endpoint, or with the user being redirected back to the client's registered redirect_uri with a correct error response.",
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
public class FAPI2SPID2EnsureUnsignedRequestAtParEndpointFails extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		isSignedRequest = false;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectAuthorizationRequestWithoutRequestObjectErrorPage.class, "FAPI2-MS-ID1-5.3.2-1");

		env.putString("error_callback_placeholder", env.getString("request_unverifiable_error"));
	}

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");

		fireTestFinished();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidRequestUri.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();
	}
}
