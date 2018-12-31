package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureMatchingKeyInAuthorizationRequestCodeIdToken extends AbstractOBEnsureMatchingKeyInAuthorizationRequest {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
