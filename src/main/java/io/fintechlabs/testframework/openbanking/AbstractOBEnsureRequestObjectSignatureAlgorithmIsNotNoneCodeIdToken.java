package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNoneCodeIdToken extends AbstractOBEnsureRequestObjectSignatureAlgorithmIsNotNone {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}
}
