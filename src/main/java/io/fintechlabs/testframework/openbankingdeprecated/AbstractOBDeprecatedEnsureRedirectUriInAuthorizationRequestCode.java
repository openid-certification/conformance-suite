package io.fintechlabs.testframework.openbankingdeprecated;

import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;

public abstract class AbstractOBDeprecatedEnsureRedirectUriInAuthorizationRequestCode extends AbstractOBDeprecatedEnsureRedirectUriInAuthorizationRequest {

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
