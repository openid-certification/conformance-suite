package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Checks the timestamps of the registration certificate in {@code issuer_info}: {@code iat} is
 * present and not in the future, and the optional {@code exp} is in the future and not later than
 * 12 months after {@code iat}.
 */
public class VCIValidateIssuerInfoRegistrationCertificateTimestamps extends AbstractCondition {

	private static final long ALLOWED_CLOCK_SKEW_SECONDS = 5 * 60;

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject payload = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload").getAsJsonObject();
		long now = Instant.now().getEpochSecond();

		JsonElement iatEl = payload.get("iat");
		if (iatEl == null || !iatEl.isJsonPrimitive() || !iatEl.getAsJsonPrimitive().isNumber()) {
			throw error("The registration certificate in 'issuer_info' has no numeric 'iat'", args("payload", payload));
		}
		long iat = OIDFJSON.getNumber(iatEl).longValue();
		if (iat > now + ALLOWED_CLOCK_SKEW_SECONDS) {
			throw error("The 'iat' of the registration certificate in 'issuer_info' is in the future",
				args("iat", iat, "now", now));
		}

		JsonElement expEl = payload.get("exp");
		if (expEl == null) {
			logSuccess("The registration certificate in 'issuer_info' has a valid 'iat' and no 'exp', which is optional",
				args("iat", iat));
			return env;
		}
		if (!expEl.isJsonPrimitive() || !expEl.getAsJsonPrimitive().isNumber()) {
			throw error("The 'exp' of the registration certificate in 'issuer_info' is not a number", args("exp", expEl));
		}
		long exp = OIDFJSON.getNumber(expEl).longValue();
		if (exp + ALLOWED_CLOCK_SKEW_SECONDS < now) {
			throw error("The registration certificate in 'issuer_info' has expired", args("exp", exp, "now", now));
		}
		long latestExp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneOffset.UTC).plusMonths(12).toEpochSecond();
		if (exp > latestExp) {
			throw error("The 'exp' of the registration certificate in 'issuer_info' is more than 12 months after its 'iat'",
				args("iat", iat, "exp", exp, "latest_allowed_exp", latestExp));
		}

		logSuccess("The registration certificate in 'issuer_info' has a valid 'iat' and 'exp'", args("iat", iat, "exp", exp));
		return env;
	}
}
