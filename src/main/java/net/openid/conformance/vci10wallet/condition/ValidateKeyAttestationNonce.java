package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Validates the nonce claim in a key attestation JWT against any c_nonce that was issued
 * via the credential nonce endpoint.
 *
 * Removes {@code credential_nonce_response} from env on success so a single c_nonce can
 * only be used once.
 */
public class ValidateKeyAttestationNonce extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject claims = keyAttestationJwt.getAsJsonObject("claims");
		JsonElement nonceEl = claims != null ? claims.get("nonce") : null;
		String nonce = (nonceEl != null && !nonceEl.isJsonNull()) ? OIDFJSON.getString(nonceEl) : null;

		JsonObject credentialNonceResponse = env.getObject("credential_nonce_response");
		if (credentialNonceResponse == null || !credentialNonceResponse.has("c_nonce")) {
			if (nonce != null) {
				log("Found unexpected nonce in key attestation, but the nonce endpoint was not called prior.");
			} else {
				log("No c_nonce was issued and key attestation does not contain a nonce — OK");
			}
			return env;
		}

		String expectedNonce = OIDFJSON.getString(credentialNonceResponse.get("c_nonce"));

		if (nonce == null) {
			String errorDescription = "Key attestation did not contain a nonce value, but the nonce endpoint was called prior.";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_NONCE, errorDescription);
			throw error(errorDescription, args("c_nonce", expectedNonce));
		}

		if (!expectedNonce.equals(nonce)) {
			String errorDescription = "Key attestation did not contain the expected nonce value";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_NONCE, errorDescription);
			throw error(errorDescription, args("nonce", nonce, "c_nonce", expectedNonce));
		}

		env.removeObject("credential_nonce_response");
		logSuccess("Detected and invalidated expected nonce value in key attestation",
			args("nonce", nonce, "c_nonce", expectedNonce));
		return env;
	}
}
