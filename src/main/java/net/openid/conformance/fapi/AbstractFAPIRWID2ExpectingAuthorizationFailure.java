package net.openid.conformance.fapi;

public abstract class AbstractFAPIRWID2ExpectingAuthorizationFailure extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirectAndWaitForErrorCallback();
	}
}
