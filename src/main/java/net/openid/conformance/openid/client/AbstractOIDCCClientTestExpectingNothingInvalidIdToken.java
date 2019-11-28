package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.TestFailureException;

public abstract class AbstractOIDCCClientTestExpectingNothingInvalidIdToken extends AbstractOIDCCClientTest
{
	protected abstract String getAuthorizationCodeGrantTypeErrorMessage();
	protected abstract String getHandleUserinfoEndpointRequestErrorMessage();

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId)
	{
		Object returnValue = super.handleAuthorizationEndpointRequest(requestId);
		if(isAuthorizationCodeRequestUnexpected()) {
			waitForPlaceHolderToUploadLogFileOrScreenshot();
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
			waitForPlaceHolderToUploadLogFileOrScreenshot();
		}
		return super.authorizationCodeGrantType(requestId);
	}

	@Override
	protected Object handleUserinfoEndpointRequest(String requestId) {
		throw new TestFailureException(getId(), getHandleUserinfoEndpointRequestErrorMessage());
	}
}
