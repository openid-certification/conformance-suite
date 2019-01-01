package io.fintechlabs.testframework.openbanking;

public abstract class AbstractOBEnsureServerHandlesNonMatchingIntentIdCodeIdToken extends AbstractOBEnsureServerHandlesNonMatchingIntentId {

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
