package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;

public class VCIValidateAttestedKeysInKeyAttestationFromJwtProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"proof_jwt", "credential_configuration"})
	public Environment evaluate(Environment env) {

		JsonElement keyAttestationJwtEl = env.getElementFromObject("vci", "key_attestation_jwt");
		if (keyAttestationJwtEl == null) {
			log("Skipping key_attestation_jwt validation, as it is not present in the environment");
			return env;
		}

		// Wait until https://github.com/openid/OpenID4VC-HAIP/issues/360 is resolved
		// TODO handle kid: if kid is present in proof jwt header -> check kid in key_attestation_jwt->attested_keys jwk.kid header matches kid

		JsonObject jwkObj = env.getElementFromObject("proof_jwt", "jwk").getAsJsonObject();
		JWK walletPublicKey;
		try {
			walletPublicKey = JWK.parse(jwkObj.toString());
		} catch (ParseException e) {
			throw error("Failed to parse jwk from proof_jwt", e);
		}

		JsonObject credentialConfiguration = env.getObject("credential_configuration");

		JsonObject keyAttestationJwt = keyAttestationJwtEl.getAsJsonObject();
		JsonElement attestedKeysEl = env.getElementFromObject("vci", "key_attestation_jwt.claims.attested_keys");
		if (attestedKeysEl == null) {
			String errorDescription = "Proof key_attestation validation failed: required attested_keys missing in key_attestation_jwt for proof type: jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration, "key_attestation_jwt", keyAttestationJwt));
		}

		if (!attestedKeysEl.isJsonArray() || attestedKeysEl.getAsJsonArray().isEmpty()) {
			String errorDescription = "Proof key_attestation validation failed: required attested_keys is not a JsonArray or empty in key_attestation_jwt for proof type: jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration, "key_attestation_jwt", keyAttestationJwt));
		}

		JsonElement currentAttestedKeyEl = null;
		boolean foundMatchingAttestedKey = false;
		try {
			JsonArray attestedKeysArray = attestedKeysEl.getAsJsonArray();
			for (int i = 0; i < attestedKeysArray.size(); i++) {
				currentAttestedKeyEl = attestedKeysArray.get(i);
				JWK attestedKeyJwk = JWK.parse(currentAttestedKeyEl.toString());

				boolean jwkThumbprintsAreEqual = walletPublicKey.computeThumbprint().equals(attestedKeyJwk.computeThumbprint());
				if (jwkThumbprintsAreEqual) {
					foundMatchingAttestedKey = true;
					break;
				}
			}

		} catch (ParseException | JOSEException e) {
			String errorDescription = "Proof key_attestation validation failed: Failed to parse attested_key for proof type: jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription,
				args("credential_configuration", credentialConfiguration, "key_attestation_jwt", keyAttestationJwt,
					"attested_keys", attestedKeysEl, "attested_key", currentAttestedKeyEl));
		}

		if (!foundMatchingAttestedKey) {
			String errorDescription = "Proof key_attestation validation failed: Failed to find matching attested_key for proof type: jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription,
				args("credential_configuration", credentialConfiguration, "key_attestation_jwt", keyAttestationJwt,
					"attested_keys", attestedKeysEl));
		}

		logSuccess("Successfully validated key attestation via jwk header for proof type: jwt",
			args("key_attestation_jwt", keyAttestationJwt, "proof_jwt_jwk", walletPublicKey, "matching_attested_key", currentAttestedKeyEl));

		return env;
	}
}
