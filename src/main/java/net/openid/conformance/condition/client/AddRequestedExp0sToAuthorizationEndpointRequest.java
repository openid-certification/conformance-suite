package net.openid.conformance.condition.client;

public class AddRequestedExp0sToAuthorizationEndpointRequest
	extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 0;
	}
}
