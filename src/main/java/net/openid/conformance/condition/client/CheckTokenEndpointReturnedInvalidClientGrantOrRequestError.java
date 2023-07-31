package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.http.HttpStatus;

import java.util.Map;
import java.util.Set;

public class CheckTokenEndpointReturnedInvalidClientGrantOrRequestError extends AbstractCheckTokenEndpointReturnedExpectedErrorAndHttpStatus {

	@Override
	protected Map<String, Set<Integer>> getErrorStatusMap() {
		return ImmutableMap.of(
			"invalid_request", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST),
			"invalid_grant", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST),
			"invalid_client", ImmutableSet.of(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED)
		);
	}

}
