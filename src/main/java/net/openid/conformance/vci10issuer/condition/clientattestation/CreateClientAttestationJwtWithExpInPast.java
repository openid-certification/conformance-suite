package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Variant of CreateClientAttestationJwt that sets 'exp' to 10 minutes in the past.
 * Used by negative tests that verify the authorization server rejects expired client
 * attestations.
 */
public class CreateClientAttestationJwtWithExpInPast extends CreateClientAttestationJwt {

	@Override
	protected void customizeClaims(JsonObject claims) {
		// 10 minutes in the past — beyond the 5-minute clock-skew tolerance applied by JWTUtil.
		long exp = Instant.now().minusSeconds(10 * 60).getEpochSecond();
		claims.addProperty("exp", exp);
	}
}
