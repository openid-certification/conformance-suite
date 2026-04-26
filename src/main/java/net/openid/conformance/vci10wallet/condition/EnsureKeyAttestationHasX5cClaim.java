package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Ensures the key attestation JWT header carries an x5c claim.
 *
 * Per HAIP §4.5.1, "the public key used to validate the key attestation signature
 * MUST be included in the x5c JOSE header parameter." Called only on the HAIP path.
 */
public class EnsureKeyAttestationHasX5cClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci"})
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");

		JsonElement x5cEl = header.get("x5c");
		if (x5cEl == null || !x5cEl.isJsonArray() || x5cEl.getAsJsonArray().isEmpty()) {
			String errorDescription = "Key attestation JWT header MUST contain an x5c claim per HAIP §4.5.1";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("header", header));
		}

		logSuccess("Key attestation JWT header contains x5c claim");
		return env;
	}
}
