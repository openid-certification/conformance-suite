package net.openid.conformance.fapi;

import net.openid.conformance.testmodule.TestFailureException;

public abstract class AbstractFAPIRWID2ClientExpectNothingAfterAuthorizationEndpoint extends AbstractFAPIRWID2ClientTest {

	@Override
	protected Object authorizationEndpoint(String requestId){

		Object returnValue = super.authorizationEndpoint(requestId);

		waitForPlaceHolderToUploadLogFileOrScreenshot();

		return returnValue;
	}

	protected void waitForPlaceHolderToUploadLogFileOrScreenshot() {
		setStatus(Status.WAITING);
		createPlaceholder();
		waitForPlaceholders();
	}

	protected void createPlaceholder() {
		// Use for create new placeholder in subclass
		fireTestFailure();
		throw new TestFailureException(getId(), "Placeholder must be created for test " + getName());
	}

}
