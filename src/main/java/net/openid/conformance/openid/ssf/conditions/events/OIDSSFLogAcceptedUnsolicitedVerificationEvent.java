package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Logs acceptance of an unsolicited stream verification event — a verification
 * event that carries no {@code state} claim.
 * <p>
 * Per SSF 1.0 §8.1.4-2, {@code state} is optional in a verification event and
 * a transmitter MAY deliver unsolicited verification events at any time after
 * stream creation. When the test suite is looking for a solicited (stated)
 * verification event, preceding stateless events must be tolerated — this
 * condition records that tolerance explicitly in the test log, including the
 * {@code jti} of the accepted event when available, so the iteration history
 * is visible to reviewers.
 * <p>
 * {@link OIDSSFParseSecurityEventToken} should have been called first so
 * that the token claims are available under {@code ssf.verification.token.claims}.
 */
public class OIDSSFLogAcceptedUnsolicitedVerificationEvent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String jti = null;
		JsonElement claimsEl = env.getElementFromObject("ssf", "verification.token.claims");
		if (claimsEl != null && claimsEl.isJsonObject()) {
			JsonElement jtiEl = claimsEl.getAsJsonObject().get("jti");
			if (jtiEl != null && !jtiEl.isJsonNull()) {
				jti = OIDFJSON.tryGetString(jtiEl);
			}
		}

		logSuccess("Accepted transmitter-initiated verification event without 'state'",
			args("jti", jti));

		return env;
	}
}
