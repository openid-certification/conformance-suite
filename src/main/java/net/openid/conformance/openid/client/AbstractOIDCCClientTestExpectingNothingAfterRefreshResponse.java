package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.TestFailureException;

/**
 * Base class for negative tests that return invalid refresh responses
 */
public abstract class AbstractOIDCCClientTestExpectingNothingAfterRefreshResponse extends AbstractOIDCCClientTestRefreshToken
{
	protected abstract String getHandleUserinfoEndpointRequestErrorMessage();

	/**
	 * Calling the userinfo endpoint is always unexpected
	 * @param requestId
	 * @return
	 */
	@Override
	protected Object handleUserinfoEndpointRequest(String requestId) {
		throw new TestFailureException(getId(), getHandleUserinfoEndpointRequestErrorMessage());
	}

	@Override
	protected abstract void addCustomValuesToIdTokenForRefreshResponse();

	@Override
	protected Object refreshTokenGrantType(String requestId) {
		startWaitingForTimeout();
		return super.refreshTokenGrantType(requestId);
	}
}
