package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.TestFailureException;

public abstract class AbstractOIDCCClientTestExpectingNothingInvalidIdToken extends AbstractOIDCCClientTest
{
	protected abstract String getAuthorizationCodeGrantTypeErrorMessage();
	protected abstract String getHandleUserinfoEndpointRequestErrorMessage();

	protected boolean isInvalidSignature() {
		return false;
	}

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId)
	{
		Object returnValue = super.handleAuthorizationEndpointRequest(requestId);
		if(isAuthorizationCodeRequestUnexpected()) {
			startWaitingForTimeout();
		}
		return returnValue;
	}

	protected boolean isAuthorizationCodeRequestUnexpected() {
		return responseType.includesIdToken();
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(isAuthorizationCodeRequestUnexpected()) {
			throw new TestFailureException(getId(), getAuthorizationCodeGrantTypeErrorMessage());
		} else {
			startWaitingForTimeout();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	@Override
	protected Object handleUserinfoEndpointRequest(String requestId) {
		if (isInvalidSignature()) {
			if (!responseType.includesIdToken()) {
				fireTestSkipped("The client continued and called the userinfo endpoint after receiving an id token with an invalid signature from the token endpoint. This is acceptable as clients are not required to validate the signatures on id tokens received over a TLS protected connection.");
			}
		}
		throw new TestFailureException(getId(), getHandleUserinfoEndpointRequestErrorMessage());
	}
}
