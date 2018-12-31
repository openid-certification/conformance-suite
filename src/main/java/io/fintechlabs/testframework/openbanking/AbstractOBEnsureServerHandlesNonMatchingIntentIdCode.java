package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;

public abstract class AbstractOBEnsureServerHandlesNonMatchingIntentIdCode extends AbstractOBEnsureServerHandlesNonMatchingIntentId {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.QUERY;
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCode();
	}
}
