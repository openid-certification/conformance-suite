package net.openid.conformance.condition.client;

public class EnsureAccessDeniedErrorFromAuthorizationEndpointResponse extends AbstractEnsureSpecifiedErrorFromAuthorizationEndpointResponse {

	@Override
	protected String getExpectedError() {
		return "access_denied";
	}
}
