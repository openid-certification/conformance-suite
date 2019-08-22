package io.fintechlabs.testframework.condition.client;

public class EnsureAccessDeniedErrorFromAuthorizationEndpointResponse extends AbstractEnsureSpecifiedErrorFromAuthorizationEndpointResponse {

	@Override
	protected String getExpectedError() {
		return "access_denied";
	}
}
