package net.openid.conformance.condition.common;

import java.util.Set;

public class FAPICheckKeyAlgInClientJWKs extends AbstractCheckKeyAlgInClientJWKs {

	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256", "ES256" );
	}

}
