package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSubToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("request_object_claims", "client_id");
		env.putString("request_object_claims", "sub", clientId);

		logSuccess("Added sub to request object", args("sub", clientId));

		return env;
	}
}
