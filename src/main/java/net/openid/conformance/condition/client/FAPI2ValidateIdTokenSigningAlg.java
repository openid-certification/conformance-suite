package net.openid.conformance.condition.client;

import java.util.Set;

public class FAPI2ValidateIdTokenSigningAlg extends AbstractValidateIdTokenSigningAlg {
	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256", "ES256", "EdDSA");
	}
}
