package net.openid.conformance.condition.common;

import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported;

import java.util.Arrays;
import java.util.Set;

public class FAPI2CheckKeyAlgInClientJWKs extends AbstractFAPI2CheckKeyAlgInClientJWKs {

	@Override
	protected Set<String> getPermitted() {
		return Set.copyOf(Arrays.asList(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS));
	}

}
