package net.openid.conformance.condition.common;

import java.util.Set;

public class FAPI2CheckKeyAlgInClientJWKs extends AbstractFAPI2CheckKeyAlgInClientJWKs {

	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256", "ES256", "EdDSA" );
	}

}
