package net.openid.conformance.fapi2spid2;

import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Base class for tests that return an invalid id_token, either from the authorization endpoint or the token endpoint
 * Client must stop after receiving an invalid id_token
 */
@VariantNotApplicable(parameter = FAPIClientType.class, values = "plain_oauth")
public abstract class AbstractFAPI2SPID2ClientExpectNothingAfterIdTokenIssued extends AbstractFAPI2SPID2ClientTest {
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
