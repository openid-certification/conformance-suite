package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRegisteredRedirectUriCodeIdToken extends AbstractOBEnsureRegisteredRedirectUri {

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
