package io.fintechlabs.testframework.condition.client;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class ValidateIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		JsonElement acrValue = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.acr.value");
		if (acrValue != null) {

			if (acrValue.isJsonPrimitive()) {

				// Split our requirements as per the spec
				String[] valuesNeeded = OIDFJSON.getString(acrValue).split(" ");

				// Read what the server has sent us
				JsonElement acrServerClaims = env.getElementFromObject("id_token", "claims.acr");
				if (acrServerClaims == null || !acrServerClaims.isJsonPrimitive()) {
					throw error("Missing or invalid acr value in id_token",
						args("id_token", env.getObject("id_token"), "expected", valuesNeeded));
				}
				List<String> valuesReceived = Arrays.asList(OIDFJSON.getString(acrServerClaims).split(" "));

				// Test the sets
				Boolean foundEnough = false;

				for (String singleAcrValue : valuesNeeded) {
					if (valuesReceived.contains(singleAcrValue)) {
						foundEnough = true;
						break;
					}
				}

				if (!foundEnough) {
					throw error("acr value in id_token does not match requested value", args("required", valuesNeeded, "actual", valuesReceived));
				} else {
					logSuccess("acr value in id_token is as expected", args("expected", valuesNeeded, "actual", valuesReceived));
				}

			} else {
				throw error("Invalid acr claim in request object", args("actual", acrValue));
			}
		} else {
			logSuccess("Nothing to check; no acr claim in request object");
		}
		return env;
	}
}
