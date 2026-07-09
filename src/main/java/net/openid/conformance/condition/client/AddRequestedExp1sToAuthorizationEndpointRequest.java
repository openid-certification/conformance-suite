package net.openid.conformance.condition.client;

public class AddRequestedExp1sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 1;
	}
}
