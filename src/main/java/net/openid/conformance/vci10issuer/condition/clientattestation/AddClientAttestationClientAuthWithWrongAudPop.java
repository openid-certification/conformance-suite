package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientAttestationHeaderToRequest;
import net.openid.conformance.condition.client.AddClientAttestationProofHeaderToRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Variant of AddClientAttestationClientAuthToEndpointRequest that creates the client
 * attestation pop JWT with the wrong 'aud' claim. Used for negative testing.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2-5.2.1">OAuth 2.0 Attestation-Based Client Authentication §5.2</a>
 */
public class AddClientAttestationClientAuthWithWrongAudPop extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAttestationProofJwtWithWrongAud.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-5.1");
		callAndStopOnFailure(AddClientAttestationHeaderToRequest.class, "OAuth2-ATCA07-6.1");
		callAndStopOnFailure(AddClientAttestationProofHeaderToRequest.class, "OAuth2-ATCA07-6.1");
	}
}
