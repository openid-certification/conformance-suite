package net.openid.conformance.condition.client;

import java.util.Arrays;
import java.util.Set;

public class FAPI2ValidateIdTokenSigningAlg extends AbstractValidateIdTokenSigningAlg {
	@Override
	protected Set<String> getPermitted() {
		return Set.copyOf(Arrays.asList(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS));
	}
}
