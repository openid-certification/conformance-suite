package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;

public abstract class AbstractOBEnsureRegisteredRedirectUriCodeIdToken extends AbstractOBEnsureRegisteredRedirectUri {

	@Override
	protected ResponseMode getResponseMode() {
		return ResponseMode.FRAGMENT;
	}

	@Override
	protected void createAuthorizationRequest() {

		super.createAuthorizationRequest();

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
	}

	@Override
	protected void performTokenEndpointIdTokenExtraction() {
		performTokenEndpointIdTokenExtractionCodeIdToken();
	}

}
