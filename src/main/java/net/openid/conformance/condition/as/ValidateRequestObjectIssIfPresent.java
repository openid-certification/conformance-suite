package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRequestObjectIssIfPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {
		String iss = env.getString("authorization_request_object", "claims.iss");
		if (iss == null) {
			logSuccess("Request object does not contain an 'iss' claim, which is permitted by the spec");
			return env;
		}

		String clientId = env.getString("client", "client_id");
		if (clientId.equals(iss)) {
			logSuccess("iss claim matches the client id", args("iss", iss));
			return env;
		}

		throw error("iss claim is present but does not match the client id",
			args("expected", clientId, "actual", iss));
	}

}
