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
 * Validates the {@code iat} claim of a key attestation JWT.
 *
 * Per OID4VCI Appendix D.1, {@code iat} is REQUIRED. The conformance suite further
 * checks the value is plausible: not in the future (with clock skew), and not before
 * the JWT spec era (2012).
 */
public class ValidateKeyAttestationIat extends AbstractCondition {

	private static final Instant MIN_ACCEPTED_IAT = Instant.parse("2012-01-01T00:00:00Z");

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject claims = keyAttestationJwt.getAsJsonObject("claims");
		JsonElement iatEl = claims != null ? claims.get("iat") : null;

		if (iatEl == null || iatEl.isJsonNull()) {
			String errorDescription = "Key attestation JWT is missing REQUIRED 'iat' claim (OID4VCI Appendix D.1)";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription);
		}

		Instant iat = Instant.ofEpochSecond(OIDFJSON.getLong(iatEl));
		Instant now = Instant.now();
		Instant maxAcceptedIat = now.plus(5, ChronoUnit.MINUTES);

		if (iat.isAfter(maxAcceptedIat)) {
			String errorDescription = "Key attestation 'iat' claim is in the future";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("iat", iat, "now", now, "max_accepted_iat", maxAcceptedIat));
		}

		if (iat.isBefore(MIN_ACCEPTED_IAT)) {
			String errorDescription = "Key attestation 'iat' claim is implausibly far in the past (predates JWT spec)";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_PROOF, errorDescription);
			throw error(errorDescription, args("iat", iat, "min_accepted_iat", MIN_ACCEPTED_IAT));
		}

		logSuccess("Key attestation 'iat' is within acceptable bounds", args("iat", iat, "now", now));
		return env;
	}
}
