package net.openid.conformance.fapi2spid2;

/**
 * Base class for tests that return an invalid authorization response
 * Client must stop after receiving an invalid authorization response
 */
public abstract class AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse extends AbstractFAPI2SPID2ClientTest {
	@Override
	protected void createAuthorizationEndpointResponse() {
		super.createAuthorizationEndpointResponse();
		// after this the request routers refuse all endpoints; in particular the token
		// endpoint must keep working until here, as profiles like Open Banking UK / Brazil
		// require the client to obtain a client_credentials access token and create a
		// consent before it calls the authorization endpoint
		startWaitingForTimeout();
	}

	@Override
	protected String getResponseClientMustStopAfter() {
		return "an invalid authorization response (" + getAuthorizationResponseErrorMessage() + ")";
	}

	protected abstract String getAuthorizationResponseErrorMessage();
}
