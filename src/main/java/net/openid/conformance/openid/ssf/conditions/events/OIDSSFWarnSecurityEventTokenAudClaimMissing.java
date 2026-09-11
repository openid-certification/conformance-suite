package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that a SET carries an {@code aud} claim. RFC 8417 section 2.2: "This claim is
 * RECOMMENDED", and SSF 1.0 section 4.1.8 describes its shape without requiring it, so
 * callers invoke this at WARNING severity. A present value is checked by
 * {@link OIDSSFValidateSecurityEventTokenAudClaim}. Reads the parsed SET from
 * {@code set_token.claims}.
 */
public class OIDSSFWarnSecurityEventTokenAudClaimMissing extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement audEl = env.getElementFromObject("set_token", "claims.aud");
		if (audEl == null) {
			throw error("The SET does not contain an 'aud' claim; the claim is recommended so a receiver can check the SET was meant for it",
				args("claims", env.getElementFromObject("set_token", "claims")));
		}

		logSuccess("The SET contains an 'aud' claim", args("aud", audEl));

		return env;
	}
}
