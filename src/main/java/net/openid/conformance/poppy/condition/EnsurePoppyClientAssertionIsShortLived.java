package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsurePoppyClientAssertionIsShortLived extends AbstractCondition {

	static final long MAX_LIFETIME_SECONDS = 300;

	@Override
	@PreEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {
		JsonElement iat = env.getElementFromObject("client_assertion", "claims.iat");
		JsonElement exp = env.getElementFromObject("client_assertion", "claims.exp");
		if (iat == null || exp == null || !iat.isJsonPrimitive() || !exp.isJsonPrimitive()
			|| !iat.getAsJsonPrimitive().isNumber() || !exp.getAsJsonPrimitive().isNumber()) {
			throw error("The client assertion needs numeric 'iat' and 'exp' claims to measure its lifetime",
				args("claims", env.getElementFromObject("client_assertion", "claims")));
		}
		long lifetime = OIDFJSON.getLong(exp) - OIDFJSON.getLong(iat);
		if (lifetime > MAX_LIFETIME_SECONDS) {
			throw error("The client assertion lives for " + lifetime + " seconds. Section 4.1 asks for a short-lived assertion; the Poppy guides suggest at most 5 minutes (300 seconds).",
				args("lifetime_seconds", lifetime, "iat", iat, "exp", exp));
		}
		logSuccess("The client assertion is short-lived", args("lifetime_seconds", lifetime));
		return env;
	}
}
