package net.openid.conformance.openid.client;

public class AbstractOIDCCClientTestExpectingNothingInvalidIdToken extends AbstractOIDCCClientTest
{
	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId)
	{
		Object returnValue = super.handleAuthorizationEndpointRequest(requestId);
		if(responseType.includesIdToken()) {
			startWaitingForTimeout();
		}
		return returnValue;
	}

}
