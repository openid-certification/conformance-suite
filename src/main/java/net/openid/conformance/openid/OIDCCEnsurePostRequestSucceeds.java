package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ExpectRedirectUriHasBeenCalled;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-ensure-post-request-succeeds",
	displayName = "OIDCC: ensure POST request succeeds.",
	summary = "The test makes the call to the authorization endpoint as a POST request. The authentication should " +
		"complete successfully and the authorization server is expected to call the redirect_uri URL within 30 seconds.",
	profile = "OIDCC"
)
public class OIDCCEnsurePostRequestSucceeds extends AbstractOIDCCServerTest {

	protected int SECONDS_TO_WAIT_FOR_CALLBACK = 30;
	protected boolean startingShutdown = false;

	@Override
	protected void redirect(String redirectTo) {
		browser.goToUrl(redirectTo, null, "POST");
		startWaitingForTimeout();
	}

	@Override
	protected void performRedirect() {
		performRedirect("POST");
	}

	protected void startWaitingForTimeout() {
		this.startingShutdown = true;
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(SECONDS_TO_WAIT_FOR_CALLBACK * 1000L);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				callAndContinueOnFailure(ExpectRedirectUriHasBeenCalled.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.1");
				fireTestFinished();
			}
			return "done";
		});
	}
}
