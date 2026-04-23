package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;

/**
 * HAIP-specific validation of client_metadata encryption parameters.
 *
 * Per HAIP:
 * - encrypted_response_enc_values_supported MUST contain both A128GCM and A256GCM
 * - JWKS key must use ECDH-ES (alg) with P-256 curve
 */
public class ValidateVpClientMetadataEncryptionForHaip extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		// Check encrypted_response_enc_values_supported contains both A128GCM and A256GCM
		JsonElement encValuesEl = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.encrypted_response_enc_values_supported");

		if (encValuesEl == null || !encValuesEl.isJsonArray()) {
			throw error("HAIP section 5 requires encrypted_response_enc_values_supported in client_metadata",
				args("client_metadata_field", "encrypted_response_enc_values_supported"));
		}

		JsonArray encValues = encValuesEl.getAsJsonArray();
		boolean hasA128GCM = false;
		boolean hasA256GCM = false;
		for (JsonElement v : encValues) {
			String val = OIDFJSON.getString(v);
			if ("A128GCM".equals(val)) {
				hasA128GCM = true;
			}
			if ("A256GCM".equals(val)) {
				hasA256GCM = true;
			}
		}

		if (!hasA128GCM || !hasA256GCM) {
			throw error("HAIP section 5 requires encrypted_response_enc_values_supported to contain both A128GCM and A256GCM",
				args("encrypted_response_enc_values_supported", encValues));
		}

		// Check JWKS key uses ECDH-ES with P-256
		JsonElement jwksEl = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.jwks");
		if (jwksEl == null) {
			throw error("HAIP section 5 requires encrypted responses and hence requires a jwks in client_metadata");
		}

		JsonArray keys = jwksEl.getAsJsonObject().getAsJsonArray("keys");
		if (keys == null || keys.isEmpty()) {
			throw error("client_metadata jwks has no keys. HAIP section 5 requires encrypted responses and hence requires a jwks with keys in client_metadata");
		}

		boolean foundEcdhEsP256 = false;
		for (JsonElement keyEl : keys) {
			try {
				JWK jwk = JWK.parse(keyEl.toString());
				if (jwk instanceof ECKey ecKey) {
					if ("ECDH-ES".equals(jwk.getAlgorithm() != null ? jwk.getAlgorithm().getName() : null)
						&& Curve.P_256.equals(ecKey.getCurve())) {
						foundEcdhEsP256 = true;
					}
				}
			} catch (ParseException e) {
				log("Skipping unparseable JWKS key", args("key", keyEl));
			}
		}

		if (!foundEcdhEsP256) {
			throw error("HAIP section 5 (and the 'alg' requirement in OID4VP section 8.3 requires a JWKS key with alg=ECDH-ES and P-256 curve in client_metadata",
				args("jwks", jwksEl));
		}

		logSuccess("client_metadata encryption parameters satisfy HAIP & OID4VP requirements",
			args("encrypted_response_enc_values_supported", encValues));
		return env;
	}
}
