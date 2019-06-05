package io.fintechlabs.testframework.condition.client;

public class AddRequestedExp300SToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 300;
	}
}
