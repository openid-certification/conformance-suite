package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenDoesNotContainName extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token" )
	public Environment evaluate(Environment env) {
		String name = env.getString("id_token", "claims.name");

		if (name != null) {
			// see discussion on certification email list, 10th March 2020
			throw error("Unexpectedly found name in id_token. The conformance suite did not request the 'name' claim is returned in the id_token and hence did not expect the server to include it. Technically this does not violate the specifications but it is likely a bug in the server and may result in user data being exposed in unintended ways.", args("name", name));
		}

		logSuccess("name claim not found in id_token, which is expected as it was not requested to be returned there");
		return env;
	}

}
