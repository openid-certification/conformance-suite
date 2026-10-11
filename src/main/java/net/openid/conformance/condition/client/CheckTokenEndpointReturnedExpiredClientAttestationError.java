package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.hc.core5.http.HttpStatus;

import java.util.Map;
import java.util.Set;

/**
 * Accepts the errors an authorization server may return when it rejects an expired client
 * attestation at the token endpoint. As well as the errors for any invalid attestation, this
 * accepts use_fresh_attestation: OAuth2-ATCA §6.2 requires it when the attestation "is deemed
 * to be not fresh enough", and an attestation whose exp has passed is the limiting case of that.
 */
public class CheckTokenEndpointReturnedExpiredClientAttestationError extends CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError {

	@Override
	protected Map<String, Set<Integer>> getErrorStatusMap() {
		return ImmutableMap.<String, Set<Integer>>builder()
			.putAll(super.getErrorStatusMap())
			.put("use_fresh_attestation", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED))
			.build();
	}

}
