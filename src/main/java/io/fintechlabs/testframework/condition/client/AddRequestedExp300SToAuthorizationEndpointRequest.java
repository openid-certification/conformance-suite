package io.fintechlabs.testframework.condition.client;

public class AddRequestedExp300SToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	@Override
	protected String getExpectedRequestedExpiry() {
		return "300";
	}
}
