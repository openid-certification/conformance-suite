package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidClientIdToQueryParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = "query_parameters")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("query_parameters", "client_id");
		String invalidClientId = clientId + "INVALID";
		env.putString("query_parameters", "client_id", invalidClientId);

		logSuccess("Added invalid client_id to query parameters", args("client_id", invalidClientId));

		return env;
	}

}
