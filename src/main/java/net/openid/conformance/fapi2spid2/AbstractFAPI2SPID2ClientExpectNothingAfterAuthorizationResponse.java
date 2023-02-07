package net.openid.conformance.fapi2spid2;

import net.openid.conformance.testmodule.TestFailureException;

/**
 * Base class for tests that return an invalid authorization response
 * Client must stop after receiving an invalid authorization response
 */
public abstract class AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse extends AbstractFAPI2SPID2ClientTest {
	@Override
	protected void createAuthorizationEndpointResponse() {
		super.createAuthorizationEndpointResponse();
		startWaitingForTimeout();
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an invalid authorization response (" +
			getAuthorizationResponseErrorMessage()+ ")");
	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called userinfo_endpoint after receiving an invalid authorization response (" +
			getAuthorizationResponseErrorMessage() + ")");
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving an invalid authorization response (" +
			getAuthorizationResponseErrorMessage() + ")");
	}

	protected abstract String getAuthorizationResponseErrorMessage();
}
