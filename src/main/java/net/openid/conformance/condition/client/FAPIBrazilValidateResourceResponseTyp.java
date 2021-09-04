package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateResourceResponseTyp extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"endpoint_response_jwt"})
	public Environment evaluate(Environment env) {

		String typ = env.getString("endpoint_response_jwt", "header.typ");

		if (typ == null) {
			logSuccess("Response header 'typ' is missing from response JWT", args("typ", typ));
			return env;
		}

		if (typ.equals("JWT")) {
			logSuccess("Response header 'typ' is JWT", args("typ", typ));
			return env;
		}

		throw error("Response header 'typ' must be JWT", args("typ", typ));
	}
}
