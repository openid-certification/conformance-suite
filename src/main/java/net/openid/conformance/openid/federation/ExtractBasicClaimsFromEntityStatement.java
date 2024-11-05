package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractBasicClaimsFromEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_jwt")
	@PostEnvironment(strings = { "federation_response_iss", "federation_response_sub" })
	public Environment evaluate(Environment env) {

		String iss = OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.iss"));
		String sub = OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.sub"));
		Long iat = OIDFJSON.getLong(env.getElementFromObject("federation_response_jwt", "claims.iat"));
		Long exp = OIDFJSON.getLong(env.getElementFromObject("federation_response_jwt", "claims.exp"));

		env.putString("federation_response_iss", iss);
		env.putString("federation_response_sub", sub);
		env.putLong("federation_response_iat", iat);
		env.putLong("federation_response_exp", exp);

		logSuccess("Extracted basic claims from entity statement", args("iss", iss, "sub", sub, "iat", iat, "exp", exp));

		return env;
	}

}
