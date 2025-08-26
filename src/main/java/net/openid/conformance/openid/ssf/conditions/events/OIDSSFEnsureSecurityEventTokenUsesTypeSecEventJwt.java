package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		String tokenType = env.getString("set_token", "header.typ");

		if (tokenType == null) {
			throw error("Couldn't find typ");
		}

		if (!"secevent+jwt".equals(tokenType)) {
			throw error("Invalid token type '"+tokenType+"'. Should be 'secevent+jwt'", args("typ", tokenType));
		}

		logSuccess("Valid token type 'secevent+jwt'", args("typ", tokenType));

		return env;
	}
}
