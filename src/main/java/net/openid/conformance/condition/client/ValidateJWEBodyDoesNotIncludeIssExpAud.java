package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class ValidateJWEBodyDoesNotIncludeIssExpAud extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonArray unexpected = new JsonArray();
		for (String claim: List.of("iss", "aud", "exp")) {
			if (env.getString("authorization_endpoint_response", claim) != null) {
				unexpected.add(claim);
			}
		}

		if (!unexpected.isEmpty()) {
			throw error("The JWE body contains one of more of iss/exp/aud claims", args("unexpected", unexpected));
		}

		logSuccess("The JWE body does not does not contain iss/exp/aud claims");

		return env;

	}

}
