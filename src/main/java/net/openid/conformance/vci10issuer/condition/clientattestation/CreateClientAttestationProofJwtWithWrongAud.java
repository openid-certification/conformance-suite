package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.CreateClientAttestationProofJwt;

/**
 * Variant of CreateClientAttestationProofJwt that sets 'aud' to a value that is not
 * the authorization server issuer. Used by negative tests that verify the authorization
 * server rejects proof-of-possession JWTs with the wrong audience.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2-5.2.1">OAuth 2.0 Attestation-Based Client Authentication §5.2</a>
 */
public class CreateClientAttestationProofJwtWithWrongAud extends CreateClientAttestationProofJwt {

	public static final String WRONG_AUD = "https://invalid-aud.example.org/";

	@Override
	protected void customizeClaims(JsonObject claims) {
		claims.addProperty("aud", WRONG_AUD);
	}
}
