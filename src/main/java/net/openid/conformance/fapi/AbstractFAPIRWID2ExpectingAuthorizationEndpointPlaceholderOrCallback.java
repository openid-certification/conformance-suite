package net.openid.conformance.fapi;

public abstract class AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirectAndWaitForPlaceholdersOrCallback();
	}
}
