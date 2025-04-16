package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidClientIdToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("request_object_claims", "client_id");
		String invalidClientId = clientId + "/1";
		env.putString("request_object_claims", "client_id", invalidClientId);

		logSuccess("Added invalid client_id to request object", args("client_id", invalidClientId));

		return env;
	}

}
