package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Base class for tests that return an invalid id_token, either from the authorization endpoint or the token endpoint
 * Client must stop after receiving an invalid id_token
 */
@VariantNotApplicable(parameter = FAPIClientType.class, values = "plain_oauth")
public abstract class AbstractFAPI1AdvancedFinalClientExpectNothingAfterIdTokenIssued extends AbstractFAPI1AdvancedFinalClientTest {
	protected boolean issueIdTokenCalled = false;

	@Override
	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		super.issueIdToken(isAuthorizationEndpoint);
		startWaitingForTimeout();
		issueIdTokenCalled = true;
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		//already issued an invalid id_token but the client sent a token request
		if(issueIdTokenCalled) {
			throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an invalid id_token (" +
				getIdTokenFaultErrorMessage()+ ")");
		}
		return super.tokenEndpoint(requestId);
	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called userinfo_endpoint after receiving an invalid id_token (" +
			getIdTokenFaultErrorMessage() + ")");
	}


	@Override
	protected Object accountsEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving an invalid id_token (" +
			getIdTokenFaultErrorMessage() + ")");
	}

	protected abstract String getIdTokenFaultErrorMessage();
}
