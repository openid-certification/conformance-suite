package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrUnsupportedResponseType;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.common.ExpectResponseTypeMissingErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Response-Missing.json
@PublishTestModule(
	testName = "oidcc-response-type-missing",
	displayName = "OIDCC: response type missing",
	summary = "This test sends an authorization request that is missing the response_type parameter. The authorization server should display an error saying the response type is missing, a screenshot of which should be uploaded.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl"
	}
)
public class OIDCCResponseTypeMissing extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
			.skip(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class, "Miss out the response_type"));
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectResponseTypeMissingErrorPage.class, "RFC6749-3.1.1");

		env.putString("error_callback_placeholder", env.getString("response_type_missing_error"));
	}

	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		// response must be in url query as we didn't specify a response_type
		env.mapKey("authorization_endpoint_response", "callback_query_params");

		performGenericAuthorizationEndpointErrorResponseValidation();

		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrUnsupportedResponseType.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.1");

		eventLog.endBlock();

		fireTestFinished();
	}

}
