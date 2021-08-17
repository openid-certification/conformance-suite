package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateConsentResponseTyp extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"consent_endpoint_response_jwt"})
	public Environment evaluate(Environment env) {

		String typ = env.getString("consent_endpoint_response_jwt", "header.typ");

		if (typ.equals("PS256")) {
			logSuccess("Response header 'typ' is JWT", args("typ", typ));
			return env;
		}

		throw error("Response header 'typ' must be JWT", args("typ", typ));
	}
}
