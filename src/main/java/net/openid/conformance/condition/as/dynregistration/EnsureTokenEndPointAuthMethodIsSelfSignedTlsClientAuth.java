package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsSelfSignedTlsClientAuth extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "self_signed_tls_client_auth";
	}
}
