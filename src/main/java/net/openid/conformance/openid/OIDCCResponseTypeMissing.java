package net.openid.conformance.openid;

import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.common.ExpectResponseTypeMissingErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Response-Missing.json
@PublishTestModule(
	testName = "oidcc-response-type-missing",
	displayName = "OIDCC: ensure registered redirect URI",
	summary = "This test sends an authorization request that is missing the response_type parameter. The authorization server should display an error saying the response type is missing, a screenshot of which should be uploaded.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
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

	@Override
	protected void processCallback() {
		throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client did not supply a response_type parameter in the request.");
	}

}
