package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsClientSecretBasic extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "client_secret_basic";
	}
}
