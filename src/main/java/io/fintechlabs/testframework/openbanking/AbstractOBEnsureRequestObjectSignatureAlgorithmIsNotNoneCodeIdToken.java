package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNoneCodeIdToken extends AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNone {

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}
}
