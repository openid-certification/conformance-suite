package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.CallPAREndpoint;

public abstract class AbstractFAPI2SPID2PARExpectingAuthorizationEndpointPlaceholderOrCallback extends AbstractFAPI2SPID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		processParErrorResponse();

		fireTestFinished();
	}

	protected abstract void processParErrorResponse();
}
