package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckForClientAssertionJtiReuse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion")
	public Environment evaluate(Environment env) {

		JsonObject clientAssertion = env.getObject("client_assertion");
		if (clientAssertion == null) {
			throw error("missing client_assertion");
		}

		JsonElement jtiEl = env.getElementFromObject("client_assertion", "claims.jti");
		if (jtiEl == null) {
			throw error("jti claim missing on client_assertion", args("client_assertion", clientAssertion));
		}
		String jti = OIDFJSON.getString(jtiEl);

		String key = "client_assertion_jti_" + jti;
		if (env.getString(key) != null) {
			throw error("Detected reuse of client_assertion JWT for jti=" + jti, args("client_assertion", clientAssertion));
		}

		env.putString(key, jti);
		logSuccess("No reuse found for client_assertion JWT for jti=" + jti);

		return env;
	}
}
