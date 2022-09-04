package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRequestObjectIss extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {
		String clientId = env.getString("client", "client_id");
		String iss = env.getString("authorization_request_object", "claims.iss");
		if (iss == null) {
			throw error("Missing issuer, request object does not contain an 'iss' claim");
		}

		if (!clientId.equals(iss)) {
			throw error("Issuer mismatch, iss claim does not match the client id",
				args("expected", clientId, "actual", iss));
		}

		logSuccess("iss claim matches the client id", args("iss", iss));
		return env;
	}

}
