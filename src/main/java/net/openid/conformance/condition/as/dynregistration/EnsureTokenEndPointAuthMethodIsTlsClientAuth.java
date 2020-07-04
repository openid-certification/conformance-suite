package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsTlsClientAuth extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "tls_client_auth";
	}
}
