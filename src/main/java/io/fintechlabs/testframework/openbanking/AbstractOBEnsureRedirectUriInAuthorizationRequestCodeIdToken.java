package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRedirectUriInAuthorizationRequestCodeIdToken extends AbstractOBEnsureRedirectUriInAuthorizationRequest {

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
