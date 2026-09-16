package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.hc.core5.http.HttpStatus;

import java.util.Map;
import java.util.Set;

/**
 * Accepts the errors an authorization server may return when it rejects a client attestation
 * (or attestation proof of possession) at the token endpoint: the attestation-specific errors
 * and the generic client authentication / malformed request errors. The attestation errors and
 * invalid_client may use either 400 or 401, invalid_request must use 400.
 */
public class CheckTokenEndpointReturnedInvalidClientRequestOrAttestationError extends AbstractCheckTokenEndpointReturnedExpectedErrorAndHttpStatus {

	@Override
	protected Map<String, Set<Integer>> getErrorStatusMap() {
		return ImmutableMap.of(
			"invalid_request", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST),
			"invalid_client", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED),
			"invalid_client_attestation", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED),
			"use_fresh_attestation", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED)
		);
	}

}
