package net.openid.conformance.condition.client;

/**
 * Accepts the errors an authorization server may return when it rejects an expired client
 * attestation at the PAR endpoint. As well as the errors for any invalid attestation, this
 * accepts use_fresh_attestation: OAuth2-ATCA §6.2 requires it when the attestation "is deemed
 * to be not fresh enough", and an attestation whose exp has passed is the limiting case of that.
 */
public class CheckErrorFromParEndpointResponseErrorForExpiredClientAttestation extends CheckErrorFromParEndpointResponseError {

	public CheckErrorFromParEndpointResponseErrorForExpiredClientAttestation() {
		super("invalid_request", "invalid_client", "invalid_client_attestation", "use_fresh_attestation");
	}
}
