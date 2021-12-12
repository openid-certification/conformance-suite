package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckCallbackContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsPost;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrUnsupportedResponseType;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.common.ExpectResponseTypeMissingErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Response-Missing.json
@PublishTestModule(
	testName = "oidcc-response-type-missing",
	displayName = "OIDCC: response type missing",
	summary = "This test sends an authorization request that is missing the response_type parameter. The authorization server must either redirect back with an 'unsupported_response_type' or 'invalid_request' error, or must display an error saying the response type is missing, a screenshot of which should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCResponseTypeMissing extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.skip(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class, "Miss out the response_type");
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectResponseTypeMissingErrorPage.class, "RFC6749-3.1.1");

		env.putString("error_callback_placeholder", env.getString("response_type_missing_error"));
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (formPost) {
			env.mapKey("authorization_endpoint_response", "callback_body_form_params");
			callAndContinueOnFailure(CheckCallbackHttpMethodIsPost.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(CheckCallbackContentTypeIsFormUrlEncoded.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		} else {
			// response must be in url query as we didn't specify a response_type
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		}

		performGenericAuthorizationEndpointErrorResponseValidation();

		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrUnsupportedResponseType.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.1");

		eventLog.endBlock();

		fireTestFinished();
	}

}
