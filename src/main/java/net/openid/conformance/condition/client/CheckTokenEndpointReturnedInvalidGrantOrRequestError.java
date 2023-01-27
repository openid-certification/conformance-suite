package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpStatus;

import java.util.Map;

public class CheckTokenEndpointReturnedInvalidGrantOrRequestError extends AbstractCheckTokenEndpointReturnedExpectedErrorAndHttpStatus {

	@Override
	protected Map<String, Integer> getErrorStatusMap() {
		return ImmutableMap.of(
			"invalid_request", HttpStatus.SC_BAD_REQUEST,
			"invalid_grant", HttpStatus.SC_BAD_REQUEST
		);
	}

}
