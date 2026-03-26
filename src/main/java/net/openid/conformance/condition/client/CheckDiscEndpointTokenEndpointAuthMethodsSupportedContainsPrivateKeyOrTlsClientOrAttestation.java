package net.openid.conformance.condition.client;

import java.util.List;

public class CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsPrivateKeyOrTlsClientOrAttestation extends AbstractCheckDiscEndpointTokenEndpointAuthMethodsSupported {

	@Override
	protected List<String> getAcceptedAuthMethods() {
		return List.of("private_key_jwt", "tls_client_auth", "attest_jwt_client_auth");
	}

}
