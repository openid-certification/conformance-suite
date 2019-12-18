package net.openid.conformance.condition.client;

public class VerifyUserInfoAndIdTokenInTokenEndpointSameSub extends AbstractVerifyUserInfoAndIdTokenSameSub {

	@Override
	protected String getIdTokenKey() {
		return "token_endpoint_id_token";
	}
}
