package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRedirectUriInAuthorizationRequestCodeIdToken extends AbstractOBEnsureRedirectUriInAuthorizationRequest {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
