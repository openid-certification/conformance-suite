package io.fintechlabs.testframework.condition.client;

public class AddRequestedExp30sToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 30;
	}
}
