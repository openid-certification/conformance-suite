package net.openid.conformance.fapi2spid2;

public abstract class AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void performRedirect() {
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	protected void performNormalRedirect() {
		// some subclasses need access to the original performRedirect, this just makes it available to them
		super.performRedirect();
	}

}
