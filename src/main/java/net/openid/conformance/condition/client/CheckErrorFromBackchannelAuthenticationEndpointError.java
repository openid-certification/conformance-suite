package net.openid.conformance.condition.client;

import java.util.Set;

public class CheckErrorFromBackchannelAuthenticationEndpointError
	extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected Set<String> getExpectedErrors() {
		return Set.of("access_denied", "invalid_request", "invalid_client");
	}
}
