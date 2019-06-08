package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

import java.util.ArrayList;

public class ValidateIdTokenACRClaimAgainstRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		JsonElement requestedAcrValue = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.value");
		JsonElement requestedAcrValues = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.values");
		ArrayList<String> requestedValues = new ArrayList<String>();
		if (requestedAcrValues != null) {
			for (JsonElement value : requestedAcrValues.getAsJsonArray()) {
				String val = OIDFJSON.getString(value);
				requestedValues.add(val);
			}
		} else if (requestedAcrValue != null) {
			requestedValues.add(OIDFJSON.getString(requestedAcrValue));
		} else {
			logSuccess("Nothing to check; no acr claim in request object");
			return env;
		}

		// Read what the server has sent us
		JsonElement idTokenAcrClaim = env.getElementFromObject("id_token", "claims.acr");
		if (idTokenAcrClaim == null || !idTokenAcrClaim.isJsonPrimitive()) {
			throw error("Missing or invalid acr value in id_token",
				args("id_token", env.getObject("id_token"), "expected", requestedValues));
		}
		String idTokenValue = OIDFJSON.getString(idTokenAcrClaim);

		for (String singleAcrValue : requestedValues) {
			if (idTokenValue.equals(singleAcrValue)) {
				logSuccess("acr value in id_token is (one of) the requested values", args("requested", requestedValues, "actual", idTokenValue));
				return env;
			}
		}

		throw error("acr value in id_token is not (one of the) requested values", args("requested", requestedValues, "actual", idTokenValue));
	}
}
