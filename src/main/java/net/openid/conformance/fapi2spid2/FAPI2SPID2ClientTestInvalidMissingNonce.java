package net.openid.conformance.fapi2spid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.condition.as.RemoveNonceFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPIClientType.class, values = {"plain_oauth"})
@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-invalid-missing-nonce",
	displayName = "FAPI2-Security-Profile-ID2: client test - missing nonce in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce in the id_token is missing if the authorization request supplied one. If the client does not send a nonce value the test result will be SKIPPED.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2ClientTestInvalidMissingNonce extends AbstractFAPI2SPID2ClientTest {
	protected boolean issuedMissingNonce = false;

	@Override
	protected void endTestIfRequiredParametersAreMissing() {
		String nonce = env.getString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, CreateEffectiveAuthorizationPARRequestParameters.NONCE);
		if(Strings.isNullOrEmpty(nonce)) {
			fireTestSkipped("This test is being skipped as it relies on the client supplying an OPTIONAL nonce value - since none is supplied, this can not be tested. PKCE prevents CSRF so this is acceptable and will not prevent certification.");
		}
	}

	@Override
	protected void addCustomValuesToIdToken() {
		String nonce = env.getString("id_token_claims", "nonce");
		if(!Strings.isNullOrEmpty(nonce)) {
			callAndStopOnFailure(RemoveNonceFromIdToken.class, "OIDCC-3.1.3.7-11");
			issuedMissingNonce = true;
		}
	}

	@Override
	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		super.issueIdToken(isAuthorizationEndpoint);
		if(issuedMissingNonce) {
			startWaitingForTimeout();
		}
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		//already issued an invalid id_token but the client sent a token request
		if(issuedMissingNonce) {
			throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an invalid id_token (" +
				getIdTokenFaultErrorMessage()+ ")");
		} else {
			return super.tokenEndpoint(requestId);
		}
	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		if(issuedMissingNonce) {
			throw new TestFailureException(getId(), "Client has incorrectly called userinfo_endpoint after receiving an invalid id_token (" +
				getIdTokenFaultErrorMessage() + ")");
		} else {
			return super.userinfoEndpoint(requestId);
		}
	}


	@Override
	protected Object accountsEndpoint(String requestId) {
		if(issuedMissingNonce) {
			throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving an invalid id_token (" +
				getIdTokenFaultErrorMessage() + ")");
		} else {
			return super.accountsEndpoint(requestId);
		}
	}

	protected String getIdTokenFaultErrorMessage() {
		return "missing nonce value";
	}
}
