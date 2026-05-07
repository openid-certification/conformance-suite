package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Verifies the key attestation JWT header `alg` is `ES256` per HAIP §4.5.1.
 */
public class EnsureKeyAttestationAlgIsES256 extends AbstractCondition {

	private static final String EXPECTED_ALG = "ES256";

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");

		String actual = header.has("alg") ? OIDFJSON.getString(header.get("alg")) : null;
		if (!EXPECTED_ALG.equals(actual)) {
			String errorDescription = "Key attestation JWT header 'alg' must be '" + EXPECTED_ALG + "'";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("expected", EXPECTED_ALG, "actual", actual));
		}

		logSuccess("Key attestation JWT header 'alg' is '" + EXPECTED_ALG + "'");
		return env;
	}
}
