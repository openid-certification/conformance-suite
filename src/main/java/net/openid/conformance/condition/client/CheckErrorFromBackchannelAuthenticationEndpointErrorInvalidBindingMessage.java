package net.openid.conformance.condition.client;

import java.util.Set;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected Set<String> getExpectedErrors() {
		return Set.of("invalid_binding_message");
	}
}
