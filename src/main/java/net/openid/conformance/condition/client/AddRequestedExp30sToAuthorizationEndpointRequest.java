package net.openid.conformance.condition.client;

public class AddRequestedExp30sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected String getExpectedRequestedExpiry() {
		return "30";
	}
}
