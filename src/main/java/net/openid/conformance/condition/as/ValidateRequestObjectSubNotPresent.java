package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRequestObjectSubNotPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		JsonElement sub = env.getElementFromObject("authorization_request_object", "claims.sub");
		if (sub != null) {
			throw error("The sub claim must not be present. This prevents reuse of the statement for private_key_jwt client authentication.");
		}

		logSuccess("sub claim is not present");

		return env;
	}

}
