package net.openid.conformance.fapi2spfinal;

public abstract class AbstractFAPI2SPFinalExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performRedirect() {
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	protected void performNormalRedirect() {
		// some subclasses need access to the original performRedirect, this just makes it available to them
		super.performRedirect();
	}

}
