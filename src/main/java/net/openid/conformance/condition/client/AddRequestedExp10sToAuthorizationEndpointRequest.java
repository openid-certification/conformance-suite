package net.openid.conformance.condition.client;

public class AddRequestedExp10sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 10;
	}
}
