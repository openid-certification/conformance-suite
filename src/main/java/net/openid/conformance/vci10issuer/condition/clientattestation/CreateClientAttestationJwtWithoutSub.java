package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;

/**
 * Variant of CreateClientAttestationJwt that omits the 'sub' claim.
 * Used by negative tests that verify the authorization server rejects client
 * attestations that do not identify the client via 'sub'.
 */
public class CreateClientAttestationJwtWithoutSub extends CreateClientAttestationJwt {

	@Override
	protected void customizeClaims(JsonObject claims) {
		claims.remove("sub");
	}
}
