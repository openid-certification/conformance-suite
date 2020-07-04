package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsClientSecretJwt extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "client_secret_jwt";
	}
}
