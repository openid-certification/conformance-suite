package io.fintechlabs.testframework.condition.client;

public class AddRequestedExp30sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected String getExpectedRequestedExpiry() {
		return "30";
	}
}
