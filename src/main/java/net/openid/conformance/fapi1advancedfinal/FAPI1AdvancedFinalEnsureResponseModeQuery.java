package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckAuthorizationResponseWhenResponseModeQuery;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectResponseModeQueryErrorPage;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToQuery;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-response-mode-query",
	displayName = "FAPI1-Advanced-Final: ensure response_mode query",
	summary = "This test includes response_mode=query in the authorization request. The authorization server should show an error message that response_mode=query is not permitted for FAPI1 Advanced (a screenshot of which should be uploaded), should return an error to the client, or must successfully authenticate without returning the result in the query.\n\nThis requirement comes from https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#Combinations - 'the query encoding MUST NOT be used'\n\nOr, if response_type=code is in use, then response_mode=jwt is required by FAPI1 Advanced.",
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
public class FAPI1AdvancedFinalEnsureResponseModeQuery extends AbstractFAPI1AdvancedFinalExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectResponseModeQueryErrorPage.class, "OAuth2-RT-5");

		env.putString("error_callback_placeholder", env.getString("response_mode_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {

		return super.makeCreateAuthorizationRequestSteps()
				.then(condition(SetAuthorizationEndpointRequestResponseModeToQuery.class));
	}

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "JAR-6.2");

		fireTestFinished();
	}

	@Override
	protected void processCallback() {

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		// This call will map authorization_endpoint_response onto callback_query_params or callback_params depending
		// what response the server decided to return and where
		callAndStopOnFailure(CheckAuthorizationResponseWhenResponseModeQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		if (authorizationEndpointResponse.has("error")) {

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");

		}

		fireTestFinished();
	}
}
