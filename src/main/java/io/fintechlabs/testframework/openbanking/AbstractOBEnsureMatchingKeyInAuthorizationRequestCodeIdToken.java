package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureMatchingKeyInAuthorizationRequestCodeIdToken extends AbstractOBEnsureMatchingKeyInAuthorizationRequest {

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
