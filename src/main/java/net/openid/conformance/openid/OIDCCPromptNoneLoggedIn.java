package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-prompt-none-LoggedIn.json
@PublishTestModule(
	testName = "oidcc-prompt-none-logged-in",
	displayName = "OIDCC: prompt=none when logged in",
	summary = "This test calls the authorization endpoint test twice. The second time it will include prompt=none, and the authorization server must not request that the user logs in. The test verifies that auth_time (if present) and sub are consistent between the id_tokens from the first and second authorizations.",
	profile = "OIDCC"
)
public class OIDCCPromptNoneLoggedIn extends AbstractOIDCCSameAuthTwiceServerTest {

	@Override
	protected void createSecondAuthorizationRequest() {
		// with prompt=none this time
		call(createAuthorizationRequestSequence()
			.then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
	}

}
