package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Verifies the key attestation JWT header `typ` is `key-attestation+jwt`
 * per OID4VCI Appendix D.1.
 */
public class EnsureKeyAttestationTypIsKeyAttestationJwt extends AbstractCondition {

	private static final String EXPECTED_TYP = "key-attestation+jwt";

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");

		String actual = header.has("typ") ? OIDFJSON.getString(header.get("typ")) : null;
		if (!EXPECTED_TYP.equals(actual)) {
			String errorDescription = "Key attestation JWT header 'typ' must be '" + EXPECTED_TYP + "'";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("expected", EXPECTED_TYP, "actual", actual));
		}

		logSuccess("Key attestation JWT header 'typ' is '" + EXPECTED_TYP + "'");
		return env;
	}
}
