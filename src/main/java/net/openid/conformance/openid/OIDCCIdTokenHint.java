package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddIdTokenHintFromFirstLoginToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_id_token_hint
// This essentially only checks that the server doesn't reject a valid id_token_hint; it is otherwise identical
// to the prompt=none whilst logged in test
@PublishTestModule(
	testName = "oidcc-id-token-hint",
	displayName = "OIDCC: id_token hint",
	summary = "This test calls the authorization endpoint test twice. The second time it will include prompt=none with the id_token_hint set to the id token from the first authorization, and the authorization server must return successfully immediately without interacting with the user. The test verifies that auth_time (if present) and sub are consistent between the id_tokens from the first and second authorizations.",
	profile = "OIDCC"
)
public class OIDCCIdTokenHint extends AbstractOIDCCSameAuthTwiceServerTest {

	@Override
	protected void createSecondAuthorizationRequest() {
		call(createAuthorizationRequestSequence()
			.then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1"))
			.then(condition(AddIdTokenHintFromFirstLoginToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-3.1.2.2")));
	}

}
