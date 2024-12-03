package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFEnsureEventSignedWithRsa256 extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		String tokenSignatureAlg = env.getString("ssf", "verification.token.header.alg");

		if (tokenSignatureAlg == null) {
			throw error("Couldn't find tokenSignatureAlg");
		}

		if (!"RS256".equals(tokenSignatureAlg)) {
			throw error("Invalid token signature algorithm '"+tokenSignatureAlg+"'. Should be 'RS256'", args("alg", tokenSignatureAlg));
		}

		logSuccess("Valid token signature algorithm 'RS256'", args("alg", tokenSignatureAlg));

		return env;
	}
}
