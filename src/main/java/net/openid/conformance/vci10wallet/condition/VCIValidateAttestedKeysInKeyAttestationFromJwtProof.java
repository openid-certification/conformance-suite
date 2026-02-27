package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;

public class VCIValidateAttestedKeysInKeyAttestationFromJwtProof extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "proof_jwt", strings = {"proof_type"})
	public Environment evaluate(Environment env) {

		JsonElement proofJwtJwkEl = env.getElementFromObject("proof_jwt", "header.jwk");
		JsonElement proofJwtKidEl = env.getElementFromObject("proof_jwt", "header.kid");
		JsonElement proofJwtX5cEl = env.getElementFromObject("proof_jwt", "header.x5c");

		if (!(proofJwtJwkEl == null ^ proofJwtKidEl == null ^ proofJwtX5cEl == null)) {
			throw error("proof_jwt header must contain exactly one of jwk, kid, or x5c");
		}

		String proofType = env.getString("proof_type");
		JsonObject proofTypeObject = env.getObject("proof_type_object");
		if (proofTypeObject == null) {
			throw error("proof_type_object is missing in environment");
		}

		JsonObject credentialConfiguration = env.getObject("credential_configuration");

		JsonObject keyAttestationRequired = proofTypeObject.getAsJsonObject("key_attestations_required");
		String keyAttestationJwt = env.getString("proof_jwt", "header.key_attestation");
		if (keyAttestationRequired == null) {
			if (keyAttestationJwt != null) {
				// key_attestation is present in header but not required
				log("Found unexpected key_attestation in proof jwt header. Key attestation is not required for current credential configuration",
					args("key_attestation", keyAttestationJwt, "credential_configuration", credentialConfiguration));
			}
		} else if (keyAttestationJwt == null) {
			// key_attestation is NOT present in header but required
			String errorDescription = "Proof key_attestation validation failed: required key_attestation missing in header for proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration));
		}

		// parse key_attestation raw jwt into an object
		extractJWT(env, "proof_jwt", "header.key_attestation", "key_attestation_jwt");

		// extract result key attestation object
		JsonObject keyAttestation = env.getObject("key_attestation_jwt");

		JsonElement attestedKeysEl = env.getElementFromObject("key_attestation_jwt", "claims.attested_keys");
		if (attestedKeysEl == null) {
			String errorDescription = "Proof key_attestation validation failed: required attested_keys missing in key_attestation_jwt for proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration, "key_attestation", keyAttestation));
		}

		if (!attestedKeysEl.isJsonArray() || attestedKeysEl.getAsJsonArray().isEmpty()) {
			String errorDescription = "Proof key_attestation validation failed: required attested_keys is not an JsonArray or empty in key_attestation_jwt for proof type: " + proofType;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("credential_configuration", credentialConfiguration, "key_attestation", keyAttestation));
		}

		// attempt to validate attested keys via jwk from proof jwt
		if (proofJwtJwkEl != null) {

			JsonObject proofJwtJwkObj = proofJwtJwkEl.getAsJsonObject();
			JsonElement currentAttestedKeyEl = null;
			boolean foundMatchingAttestedKey = false;
			try {
				JWK proofJwtJwk = JWK.parse(proofJwtJwkObj.toString());
				JsonArray attestedKeysArray = attestedKeysEl.getAsJsonArray();
				for (int i = 0; i < attestedKeysArray.size(); i++) {
					currentAttestedKeyEl = attestedKeysArray.get(i);
					if (currentAttestedKeyEl.isJsonObject()) {
						JsonObject currentAttestedKey = currentAttestedKeyEl.getAsJsonObject();
						JWK attestedKeyJwk = JWK.parse(currentAttestedKey.toString());

						boolean jwksAreEqual = proofJwtJwk.computeThumbprint().equals(attestedKeyJwk.computeThumbprint());
						if (jwksAreEqual) {
							foundMatchingAttestedKey = true;
							break;
						}
					}
				}

			} catch (ParseException | JOSEException e) {
				String errorDescription = "Proof key_attestation validation failed: Failed to parse attested_key for proof type: " + proofType;
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("credential_configuration", credentialConfiguration, "key_attestation", keyAttestation,
						"attested_keys", attestedKeysEl, "attested_key", currentAttestedKeyEl));
			}

			if (!foundMatchingAttestedKey) {
				String errorDescription = "Proof key_attestation validation failed: Failed to find matching attested_key for proof type: " + proofType;
				VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
				throw error(errorDescription,
					args("credential_configuration", credentialConfiguration, "key_attestation", keyAttestation,
						"attested_keys", attestedKeysEl));
			}

			logSuccess("Successfully validated key attestation via jwk header for proof type: " + proofType,
				args("key_attestation", keyAttestation, "proof_jwt_jwk", proofJwtJwkEl, "matching_attested_key", currentAttestedKeyEl));
		} else {

			// Wait until https://github.com/openid/OpenID4VC-HAIP/issues/360 is resolved
			// TODO x5c: for x5c do the same check for jwk
			// TODO kid: if kid is present in proof jwt header -> check kid in key_attestation_jwt->attested_keys jwk.kid header matches kid

			throw error("Support for attested keys validation via x5c or kid is not implemented yet.");
		}


		return env;
	}
}
