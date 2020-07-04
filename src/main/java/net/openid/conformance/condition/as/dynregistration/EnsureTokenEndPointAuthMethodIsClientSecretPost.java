package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsClientSecretPost extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "client_secret_post";
	}
}
