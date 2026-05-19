package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Ensures the key attestation JWT carries an {@code exp} claim when used with the
 * JWT proof type.
 *
 * Per OID4VCI Appendix D.1, {@code exp} "MUST be present if the attestation is used
 * with the JWT proof type" — i.e., when nested inside a {@code proof_type=jwt}
 * proof's JOSE header. Wired only on that path.
 */
public class EnsureKeyAttestationExpIsPresentForJwtProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject claims = keyAttestationJwt.getAsJsonObject("claims");
		JsonElement expEl = claims != null ? claims.get("exp") : null;

		if (expEl == null || expEl.isJsonNull()) {
			String errorDescription = "Key attestation used with JWT proof type MUST contain 'exp' claim per OID4VCI Appendix D.1";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription);
		}

		logSuccess("Key attestation contains 'exp' claim as required for JWT proof type");
		return env;
	}
}
