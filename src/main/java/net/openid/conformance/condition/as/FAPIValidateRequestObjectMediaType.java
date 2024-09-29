package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIValidateRequestObjectMediaType extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String typ = env.getString("authorization_request_object", "header.typ");

		if (typ == null) {
			logSuccess("Request object media type was not specified");
			return env;
		}

		if (typ.equalsIgnoreCase("oauth-authz-req+jwt")) {
			logSuccess("Request object media type is valid", args("typ", typ));
			return env;
		}

		throw error("Request object media type is not as expected", args("expected", "oauth-authz-req+jwt", "typ", typ));
	}
}
