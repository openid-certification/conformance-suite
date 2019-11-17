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
