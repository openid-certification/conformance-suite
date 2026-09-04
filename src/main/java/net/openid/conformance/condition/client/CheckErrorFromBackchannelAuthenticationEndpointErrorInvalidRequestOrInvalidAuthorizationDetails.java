package net.openid.conformance.condition.client;

import java.util.Set;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails
	extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected Set<String> getExpectedErrors() {
		return Set.of("invalid_request", "invalid_authorization_details");
	}
}
