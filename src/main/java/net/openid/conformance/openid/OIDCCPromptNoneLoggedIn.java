package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckIdTokenAuthTimeClaimsSameIfPresent;
import net.openid.conformance.condition.client.CheckIdTokenSubConsistentForSecondAuthorization;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-prompt-none-LoggedIn.json
@PublishTestModule(
	testName = "oidcc-prompt-none-logged-in",
	displayName = "OIDCC: prompt=none when logged in",
	summary = "This test calls the authorization endpoint test twice. The second time it will include prompt=none, and the authorization server must not request that the user logs in. The test verifies that auth_time (if present) and sub are consistent between the id_tokens from the first and second authorizations.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl"
	}
)
public class OIDCCPromptNoneLoggedIn extends AbstractOIDCCServerTest {
	private boolean firstTime = true;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void createAuthorizationRequest() {
		if (firstTime) {
			// capture id_token from first authentication for later comparison (we don't care if it's from
			// the authorization endpoint or the token endpoint)
			env.mapKey("id_token", "first_id_token");
			super.createAuthorizationRequest();
		} else {
			env.unmapKey("id_token");
			call(new CreateAuthorizationRequestSteps()
				.then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
		}
	}

	protected void onPostAuthorizationFlowComplete() {
		if (firstTime) {
			firstTime = false;
			// do the process again, but with prompt=none this time
			performAuthorizationFlow();
		} else {
			// these two checks are equivalent to same-authn, https://github.com/rohe/oidctest/blob/a306ff8ccd02da456192b595cf48ab5dcfd3d15a/src/oidctest/op/check.py#L1117

			// this check only works if the server actually returns auth_time; it might be better to explicitly request auth_time
			// we could also time how long the second authorization endpoint call takes; it should really only take seconds as it
			// should just redirect straight back instantly.
			callAndContinueOnFailure(CheckIdTokenAuthTimeClaimsSameIfPresent.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
			callAndContinueOnFailure(CheckIdTokenSubConsistentForSecondAuthorization.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

			fireTestFinished();
		}
	}

	@Override
	public void cleanup() {
		firstTime = true; // to avoid any blocks created in cleanup being prefixed in currentClientString()
		super.cleanup();
	}
}
