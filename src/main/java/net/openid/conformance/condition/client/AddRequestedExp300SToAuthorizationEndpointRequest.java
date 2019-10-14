package net.openid.conformance.condition.client;

public class AddRequestedExp300SToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected String getExpectedRequestedExpiry() {
		return "300";
	}
}
