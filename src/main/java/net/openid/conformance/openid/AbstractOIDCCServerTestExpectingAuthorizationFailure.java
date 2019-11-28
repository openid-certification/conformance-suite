package net.openid.conformance.openid;

public class AbstractOIDCCServerTestExpectingAuthorizationFailure extends AbstractOIDCCServerTest {

	@Override
	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirectAndWaitForErrorCallback();
		eventLog.endBlock();
	}

}
