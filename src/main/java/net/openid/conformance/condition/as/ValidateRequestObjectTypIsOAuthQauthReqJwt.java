package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRequestObjectTypIsOAuthQauthReqJwt extends AbstractCondition {

	public static final String REQ_OBJ_TYP = "oauth-authz-req+jwt";

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String typ = env.getString("authorization_request_object", "header.typ");

		if (typ == null) {
			throw error("typ is missing in request object header", args("expected"));
		}

		if (typ.equalsIgnoreCase(REQ_OBJ_TYP)) {
			logSuccess("Request object typ header is valid", args("typ", typ));
			return env;
		}

		throw error("Request object typ header is not as expected", args("expected", REQ_OBJ_TYP, "actual", typ));
	}
}
