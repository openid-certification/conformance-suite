package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddMaxAge1ToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckIdTokenAuthTimeClaimPresentDueToMaxAge;
import net.openid.conformance.condition.client.CheckIdTokenAuthTimeIsRecentIfPresent;
import net.openid.conformance.condition.client.CheckSecondIdTokenAuthTimeIsLaterIfPresent;
import net.openid.conformance.condition.client.ExpectSecondLoginPage;
import net.openid.conformance.condition.client.WaitFor2Seconds;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_max_age=1
// https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Req-max_age=1.json
@PublishTestModule(
	testName = "oidcc-max-age-1",
	displayName = "OIDCC: max-age=1",
	summary = "This test calls the authorization endpoint test twice. The second time it waits 1 second and includes max_age=1, so that the authorization server is required to ask the user to login a second time and must return an auth_time claim in the second id_token. A screenshot of the second authorization should be uploaded.",
	profile = "OIDCC"
)
public class OIDCCMaxAge1 extends AbstractOIDCCServerTest {
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
			// we're sending max_age=2, so after 1 second the previous authentication is just still valid - so wait for
			// 2 seconds
			callAndStopOnFailure(WaitFor2Seconds.class);
			call(createAuthorizationRequestSequence()
				.then(condition(AddMaxAge1ToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
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
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		if (!firstTime) {
			callAndContinueOnFailure(CheckIdTokenAuthTimeClaimPresentDueToMaxAge.class, Condition.ConditionResult.FAILURE, "OIDCC-2", "OIDCC-3.1.2.1");
			callAndContinueOnFailure(CheckSecondIdTokenAuthTimeIsLaterIfPresent.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
			callAndContinueOnFailure(CheckIdTokenAuthTimeIsRecentIfPresent.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (firstTime) {
			firstTime = false;
			// do the process again, but with prompt=login this time
			performAuthorizationFlow();
		} else {
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
