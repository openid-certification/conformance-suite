package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPromptLoginToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckSecondIdTokenAuthTimeIsLaterIfPresent;
import net.openid.conformance.condition.client.ExpectSecondLoginPage;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-prompt-login.json
@PublishTestModule(
	testName = "oidcc-prompt-login",
	displayName = "OIDCC: prompt=login",
	summary = "This test calls the authorization endpoint test twice. The second time it will include prompt=login, so that the authorization server is required to ask the user to login a second time. If auth_time is present in the id_tokens, the value from the second login must be later than the time in the original token. A screenshot of the second authorization should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCPromptLogin extends AbstractOIDCCServerTest {
	private boolean firstTime = true;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void createPlaceholder() {
		// asking the user for a screenshot of the second login seems a little pointless as there's no way anyone
		// can verify it's from the second login, not the first.
		// It would seem more sensible to have tests that make essential claims for auth_time
		callAndStopOnFailure(ExpectSecondLoginPage.class, "OIDCC-3.1.2.1");
		env.putString("error_callback_placeholder", env.getString("expect_second_login_page"));
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
			// make sure the auth definitely happens at least 1 second after the original one, so auth_time will be different
			callAndStopOnFailure(WaitForOneSecond.class);
			call(createAuthorizationRequestSequence()
				.then(condition(AddPromptLoginToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
		}
	}

	@Override
	protected final void performRedirect() {
		if (firstTime) {
			super.performRedirect();
		} else {
			super.performRedirectWithPlaceholder();
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (firstTime) {
			firstTime = false;
			// do the process again, but with prompt=login this time
			performAuthorizationFlow();
		} else {
			callAndContinueOnFailure(CheckSecondIdTokenAuthTimeIsLaterIfPresent.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

			setStatus(Status.WAITING);
			waitForPlaceholders();
		}
	}

	@Override
	public void cleanup() {
		firstTime = true; // to avoid any blocks created in cleanup being prefixed in currentClientString()
		super.cleanup();
	}
}
