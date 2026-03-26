package net.openid.conformance.condition.client;

import java.util.List;

public class CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation extends AbstractCheckDiscEndpointTokenEndpointAuthMethodsSupported {

	@Override
	protected List<String> getAcceptedAuthMethods() {
		return List.of("attest_jwt_client_auth");
	}

}
