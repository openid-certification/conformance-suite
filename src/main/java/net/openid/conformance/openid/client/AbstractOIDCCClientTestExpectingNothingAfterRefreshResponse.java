package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.TestFailureException;

/**
 * Base class for negative tests that return invalid refresh responses
 */
public abstract class AbstractOIDCCClientTestExpectingNothingAfterRefreshResponse extends AbstractOIDCCClientTestRefreshToken
{
	protected abstract String getHandleUserinfoEndpointRequestErrorMessage();

	@Override
	protected abstract void addCustomValuesToIdTokenForRefreshResponse();

	/**
	 * Calling the userinfo endpoint after receiving an invalid refresh response leads to a failure
	 * but clients are allowed to call the userinfo endpoint before the refresh request
	 * @param requestId
	 * @return
	 */
	@Override
	protected Object handleUserinfoEndpointRequest(String requestId) {
		if(receivedRefreshRequest) {
			//refresh response was invalid but the client sent a userinfo request anyway, this is wrong
			throw new TestFailureException(getId(), getHandleUserinfoEndpointRequestErrorMessage());
		} else {
			//we didn't receive a refresh request yet so the client is probably just sending a userinfo
			//request before doing the refresh part
			return super.handleUserinfoEndpointRequest(requestId);
		}
	}


	@Override
	protected Object refreshTokenGrantType(String requestId) {
		startWaitingForTimeout();
		return super.refreshTokenGrantType(requestId);
	}
}
