package net.openid.conformance.condition.client;

public class VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub extends AbstractVerifyUserInfoAndIdTokenSameSub {

	@Override
	protected String getIdTokenKey() {
		return "authorization_endpoint_id_token";
	}
}
