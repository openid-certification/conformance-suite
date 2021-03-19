package net.openid.conformance.fapirwid2;

public abstract class AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performRedirect() {
		performRedirectAndWaitForPlaceholdersOrCallback();
	}

	protected void performNormalRedirect() {
		// some subclasses need access to the original performRedirect, this just makes it available to them
		super.performRedirect();
	}

}
