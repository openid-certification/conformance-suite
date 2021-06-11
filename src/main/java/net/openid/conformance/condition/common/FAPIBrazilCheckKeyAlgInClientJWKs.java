package net.openid.conformance.condition.common;

import java.util.Set;

public class FAPIBrazilCheckKeyAlgInClientJWKs extends AbstractCheckKeyAlgInClientJWKs {

	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256" );
	}

}
