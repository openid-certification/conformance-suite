package net.openid.conformance.condition.client;

public class CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation extends AbstractCheckErrorFromTokenEndpointResponseError {

	@Override
	protected String[] getExpectedError() {
		return new String[]{"invalid_request", "invalid_client", "invalid_client_attestation", "use_fresh_attestation"};
	}

}
