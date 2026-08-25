package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Base class for tests that return an invalid id_token, either from the authorization endpoint or the token endpoint
 * Client must stop after receiving an invalid id_token
 */
@VariantNotApplicable(parameter = FAPIClientType.class, values = "plain_oauth")
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })
public abstract class AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued extends AbstractFAPI2SPFinalClientTest {
	@Override
	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		super.issueIdToken(isAuthorizationEndpoint);
		// after this the request routers refuse all endpoints, including further token
		// endpoint calls after the invalid id_token has been issued
		startWaitingForTimeout();
	}

	@Override
	protected String getResponseClientMustStopAfter() {
		return "an invalid id_token (" + getIdTokenFaultErrorMessage() + ")";
	}

	protected abstract String getIdTokenFaultErrorMessage();
}
