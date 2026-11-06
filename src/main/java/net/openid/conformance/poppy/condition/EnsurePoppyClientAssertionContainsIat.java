package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsurePoppyClientAssertionContainsIat extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {
		JsonElement iat = env.getElementFromObject("client_assertion", "claims.iat");
		if (iat == null || !iat.isJsonPrimitive() || !iat.getAsJsonPrimitive().isNumber()) {
			throw error("The client assertion has no numeric 'iat' claim, which section 4.1 requires alongside a unique 'jti'",
				args("claims", env.getElementFromObject("client_assertion", "claims")));
		}
		logSuccess("The client assertion contains 'iat'", args("iat", iat));
		return env;
	}
}
