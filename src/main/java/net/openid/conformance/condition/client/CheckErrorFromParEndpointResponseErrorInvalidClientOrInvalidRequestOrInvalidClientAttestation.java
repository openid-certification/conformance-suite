package net.openid.conformance.condition.client;

public class CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation extends CheckErrorFromParEndpointResponseError {

	public CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation() {
		super("invalid_request", "invalid_client", "invalid_client_attestation", "use_fresh_attestation");
	}
}
