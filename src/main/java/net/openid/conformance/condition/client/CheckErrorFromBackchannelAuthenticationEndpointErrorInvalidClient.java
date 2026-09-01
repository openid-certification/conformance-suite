package net.openid.conformance.condition.client;

import java.util.Set;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidClient  extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected Set<String> getExpectedErrors() {
		return Set.of("invalid_client");
	}
}
