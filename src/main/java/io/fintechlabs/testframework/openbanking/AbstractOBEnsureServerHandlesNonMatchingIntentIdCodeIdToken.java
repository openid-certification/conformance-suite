package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureServerHandlesNonMatchingIntentIdCodeIdToken extends AbstractOBEnsureServerHandlesNonMatchingIntentId {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
