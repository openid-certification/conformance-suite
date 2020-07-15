package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsPrivateKeyJwt extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "private_key_jwt";
	}
}
