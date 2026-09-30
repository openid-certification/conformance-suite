package net.openid.conformance.condition.as;

import java.util.List;

/**
 * As the parent class, for JWT assertion based client authentication (private_key_jwt), which adds
 * 'client_assertion' and 'client_assertion_type' to the form body.
 */
public class CheckForUnexpectedParametersInSignedPAREndpointRequestWithClientAssertion extends CheckForUnexpectedParametersInSignedPAREndpointRequest {

	@Override
	protected List<String> getExpectedParameters() {
		return List.of("request", "client_id", "client_assertion", "client_assertion_type");
	}

}
