package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Base class for tests that return an invalid authorization response
 * Client must stop after receiving an invalid authorization response
 */
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })
public abstract class AbstractFAPI2SPFinalClientExpectNothingAfterAuthorizationResponse extends AbstractFAPI2SPFinalClientTest {
	@Override
	protected void createAuthorizationEndpointResponse() {
		super.createAuthorizationEndpointResponse();
		// after this the request routers refuse all endpoints; in particular the token
		// endpoint must keep working until here, as profiles like KSA / Open Banking UK /
		// Brazil require the client to obtain a client_credentials access token and create
		// a consent before it calls the authorization endpoint
		startWaitingForTimeout();
	}

	@Override
	protected String getResponseClientMustStopAfter() {
		return "an invalid authorization response (" + getAuthorizationResponseErrorMessage() + ")";
	}

	protected abstract String getAuthorizationResponseErrorMessage();
}
