package net.openid.conformance.fapi;

import net.openid.conformance.testmodule.TestFailureException;

public abstract class AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint extends AbstractFAPIRWID2ClientTest {

	@Override
	protected Object authorizationEndpoint(String requestId){

		Object returnValue = super.authorizationEndpoint(requestId);

		startWaitingForTimeout();

		return returnValue;
	}

	/**
	 * Only use in tests that need to wait for a timeout
	 * As the client hasn't called an endpoint after waitTimeoutSeconds (from configuration) seconds,
	 * assume it has correctly detected the error and aborted.
	 */
	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				waitForPlaceHolderToUploadLogFileOrScreenshot();
			}
			return "done";
		});
	}

	protected void waitForPlaceHolderToUploadLogFileOrScreenshot() {
		setStatus(Status.RUNNING);
		createPlaceholder();
		setStatus(Status.WAITING);
		waitForPlaceholders();
	}

	protected void createPlaceholder() {
		// Use for create new placeholder in subclass
		throw new TestFailureException(getId(), "Placeholder must be created for test " + getName());
	}

}
