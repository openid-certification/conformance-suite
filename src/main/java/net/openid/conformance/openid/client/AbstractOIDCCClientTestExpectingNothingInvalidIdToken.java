package net.openid.conformance.openid.client;

import net.openid.conformance.condition.ConditionError;

public abstract class AbstractOIDCCClientTestExpectingNothingInvalidIdToken extends AbstractOIDCCClientTest
{
	protected abstract String getAuthorizationCodeGrantTypeErrorMessage();
	protected abstract String getHandleUserinfoEndpointRequestErrorMessage();

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId)
	{
		Object returnValue = super.handleAuthorizationEndpointRequest(requestId);
		if(responseType.includesIdToken()) {
			startWaitingForTimeout();
		}
		return returnValue;
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		if(responseType.includesIdToken()) {
			throw new ConditionError(getId(), getAuthorizationCodeGrantTypeErrorMessage());
		} else {
			startWaitingForTimeout();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	@Override
	protected Object handleUserinfoEndpointRequest(String requestId) {
		throw new ConditionError(getId(), getHandleUserinfoEndpointRequestErrorMessage());
	}
}
