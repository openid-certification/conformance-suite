package net.openid.conformance.condition.client;

import java.util.Set;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	@Override
	protected Set<String> getExpectedErrors() {
		return Set.of("invalid_login_hint");
	}
}
