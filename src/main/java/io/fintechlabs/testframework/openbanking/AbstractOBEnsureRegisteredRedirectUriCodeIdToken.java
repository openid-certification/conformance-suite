package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRegisteredRedirectUriCodeIdToken extends AbstractOBEnsureRegisteredRedirectUri {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
