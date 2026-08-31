package net.openid.conformance.condition.client;

public class AddRequestedExp86401sToAuthorizationEndpointRequest
	extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 86_401;
	}
}
