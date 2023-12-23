package net.openid.conformance.fapi1advancedfinalfapibrv1;

public abstract class AbstractFAPI1AdvancedFinalExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void performRedirect() {
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	protected void performNormalRedirect() {
		// some subclasses need access to the original performRedirect, this just makes it available to them
		super.performRedirect();
	}

}
