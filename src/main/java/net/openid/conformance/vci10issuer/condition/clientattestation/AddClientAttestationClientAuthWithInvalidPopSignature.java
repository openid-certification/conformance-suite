package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * A variant of AddClientAttestationClientAuthToEndpointRequest that invalidates
 * the client attestation pop signature. Used for negative testing.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
public class AddClientAttestationClientAuthWithInvalidPopSignature extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAttestationProofJwt.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-5.1");
		// Invalidate the pop signature after creation
		callAndContinueOnFailure(VCIInvalidateClientAttestationPopSignature.class, Condition.ConditionResult.INFO, "OAuth2-ATCA07-5.2");
		callAndStopOnFailure(AddClientAttestationHeaderToRequest.class, "OAuth2-ATCA07-6.1");
		callAndStopOnFailure(AddClientAttestationProofHeaderToRequest.class, "OAuth2-ATCA07-6.1");
	}
}
