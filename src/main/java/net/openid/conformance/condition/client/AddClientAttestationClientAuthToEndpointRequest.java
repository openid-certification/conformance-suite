package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;

/**
 * Adds client_attestation client authentication to the request whose form parameters and
 * headers are currently mapped as the client auth keys.
 *
 * The Client Attestation JWT is minted here, immediately before each authenticated request.
 * A module can sit in WAITING for a credential offer, a tx_code or the tester's login for
 * longer than the attestation's five-minute lifetime, so an attestation minted earlier could
 * be stale or expired by the time it is sent, and a rejection would then say nothing about
 * the content under test.
 */
public class AddClientAttestationClientAuthToEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAttestationJwt.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-5.1", "HAIP-4.4.1-1");
		callAndStopOnFailure(CreateClientAttestationProofJwt.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		callAndStopOnFailure(AddClientAttestationHeaderToRequest.class, "OAuth2-ATCA07-6.1");
		callAndStopOnFailure(AddClientAttestationProofHeaderToRequest.class, "OAuth2-ATCA07-6.1");
	}
}
