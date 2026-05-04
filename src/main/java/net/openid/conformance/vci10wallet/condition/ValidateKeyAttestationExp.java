package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Validates the {@code exp} claim of a key attestation JWT when present.
 *
 * Per OID4VCI Appendix D.1, {@code exp} is OPTIONAL in general; the
 * "MUST be present when used with JWT proof type" case is enforced separately by
 * {@link EnsureKeyAttestationExpIsPresentForJwtProof}. When present, the value must
 * be in the future (with clock skew) and not unreasonably far ahead.
 */
public class ValidateKeyAttestationExp extends AbstractCondition {

	private static final long MAX_EXP_DAYS_FROM_NOW = 50L * 365L;

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject claims = keyAttestationJwt.getAsJsonObject("claims");
		JsonElement expEl = claims != null ? claims.get("exp") : null;

		if (expEl == null || expEl.isJsonNull()) {
			log("Key attestation has no 'exp' claim — OK; 'exp' is OPTIONAL when not used with JWT proof type (OID4VCI Appendix D.1)");
			return env;
		}

		Instant exp = Instant.ofEpochSecond(OIDFJSON.getLong(expEl));
		Instant now = Instant.now();
		Instant minAcceptedExp = now.minus(5, ChronoUnit.MINUTES);
		Instant maxAcceptedExp = now.plus(MAX_EXP_DAYS_FROM_NOW, ChronoUnit.DAYS);

		if (exp.isBefore(minAcceptedExp)) {
			String errorDescription = "Key attestation 'exp' claim is in the past";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("exp", exp, "now", now, "min_accepted_exp", minAcceptedExp));
		}

		if (exp.isAfter(maxAcceptedExp)) {
			String errorDescription = "Key attestation 'exp' claim is unreasonably far in the future";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("exp", exp, "now", now, "max_accepted_exp", maxAcceptedExp));
		}

		logSuccess("Key attestation 'exp' is within acceptable bounds", args("exp", exp, "now", now));
		return env;
	}
}
