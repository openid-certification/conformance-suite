package net.openid.conformance.condition.client;

public class AddRequestedExp13sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 13;
	}
}
