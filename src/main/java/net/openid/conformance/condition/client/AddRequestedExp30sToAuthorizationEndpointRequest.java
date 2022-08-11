package net.openid.conformance.condition.client;

public class AddRequestedExp30sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 30;
	}
}
