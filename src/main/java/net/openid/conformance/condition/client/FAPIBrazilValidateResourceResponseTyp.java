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
			logSuccess("'typ' is missing from the header of the JWT returned in the API response", args("typ", typ));
			return env;
		}

		if (typ.equals("JWT")) {
			logSuccess("'typ' is the header of the JWT returned in the API response is 'JWT'", args("typ", typ));
			return env;
		}

		throw error("Value for 'typ' in the header of the JWT returned in the API response must be 'JWT'", args("typ", typ));
	}
}
