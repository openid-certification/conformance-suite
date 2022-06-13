package net.openid.conformance.fapi2baselineid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.condition.as.RemoveNonceFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-missing-nonce",
	displayName = "FAPI2-Baseline-ID2: client test - missing nonce in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce in the id_token is missing if the authorization_endpoint request supplies one",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestInvalidMissingNonce extends AbstractFAPI2BaselineID2ClientTest {
	protected boolean issuedMissingNonce = false;

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
