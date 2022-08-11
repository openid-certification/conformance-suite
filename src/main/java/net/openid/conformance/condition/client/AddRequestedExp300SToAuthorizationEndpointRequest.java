package net.openid.conformance.condition.client;

public class AddRequestedExp300SToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 300;
	}
}
