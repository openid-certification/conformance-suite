package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * RFC 8417 2.2: "jti ... This claim is REQUIRED" and "The identifier MUST be
 * unique within a particular event feed".
 * <p>
 * Asserts the {@code jti} claim is present and a non-empty string, and tracks
 * the jti values observed during the test run (at {@code ssf.observed_set_jtis}).
 * Seeing the same jti again with identical claims is legitimate redelivery of
 * an unacknowledged SET (RFC 8935/8936, possibly re-signed) and is logged; the
 * same jti carried by different claims is a uniqueness violation and fails the check.
 */
public class OIDSSFValidateSecurityEventTokenJtiClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"set_token", "ssf"})
	public Environment evaluate(Environment env) {

		JsonElement claimsEl = env.getElementFromObject("set_token", "claims");
		if (claimsEl == null || !claimsEl.isJsonObject()) {
			throw error("Couldn't find SET token claims");
		}

		JsonElement jtiEl = claimsEl.getAsJsonObject().get("jti");
		if (jtiEl == null) {
			throw error("jti claim is missing. RFC 8417 (2.2) requires the 'jti' claim in every SET",
				args("claims", claimsEl));
		}

		String jti = OIDFJSON.tryGetString(jtiEl);
		if (jti == null || jti.isBlank()) {
			throw error("jti claim must be a non-empty string", args("jti", jtiEl));
		}

		// Identity for redelivery detection is the claims set (JsonObject equality is
		// member-order-insensitive), not the serialized JWS: a transmitter MAY re-sign
		// or re-serialize an unacknowledged SET on redelivery (RFC 8935/8936).
		JsonObject claims = claimsEl.getAsJsonObject().deepCopy();

		JsonElement observedEl = env.getElementFromObject("ssf", "observed_set_jtis");
		JsonObject observed;
		if (observedEl != null && observedEl.isJsonObject()) {
			observed = observedEl.getAsJsonObject();
		} else {
			observed = new JsonObject();
			env.putObject("ssf", "observed_set_jtis", observed);
		}

		JsonElement previousClaimsEl = observed.get(jti);
		if (previousClaimsEl != null && !previousClaimsEl.equals(claims)) {
			throw error("The jti claim value was already used by a SET with different claims during this test. "
					+ "RFC 8417 (2.2) requires jti values to be unique within an event feed.",
				args("jti", jti));
		}

		if (previousClaimsEl != null) {
			log("Observed a redelivery of an already-seen SET (same jti, identical claims) - "
				+ "legitimate for unacknowledged SETs", args("jti", jti));
		} else {
			observed.add(jti, claims);
		}

		logSuccess("Valid jti claim present in SET claims", args("jti", jti));

		return env;
	}
}
