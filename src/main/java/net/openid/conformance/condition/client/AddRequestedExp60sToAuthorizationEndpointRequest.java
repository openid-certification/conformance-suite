package net.openid.conformance.condition.client;

public class AddRequestedExp60sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 60;
	}
}
