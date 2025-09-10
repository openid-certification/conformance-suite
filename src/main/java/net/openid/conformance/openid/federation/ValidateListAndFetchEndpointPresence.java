package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateListAndFetchEndpointPresence extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		int numberOfSubordinates = env.getInteger("number_of_subordinates_in_list_response");
		if (numberOfSubordinates > 0 && env.getString("federation_fetch_endpoint") == null) {
			throw error("An Entity with Subordinates MUST expose a fetch endpoint", args(
				"number_of_subordinates_in_list_response", numberOfSubordinates,
				"federation_fetch_endpoint", env.getString("federation_fetch_endpoint"))
			);
		}

		logSuccess("The list endpoint is accompanied by a fetch endpoint when the number of subordinates is > 0");
		return env;

	}

}
