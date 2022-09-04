package net.openid.conformance.openid;

public class AbstractOIDCCRequestObjectServerTestExpectingRedirectOrPlaceholder extends AbstractOIDCCRequestObjectServerTest {
	@Override
	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirectAndWaitForPlaceholdersOrCallback();
		eventLog.endBlock();
	}

}
