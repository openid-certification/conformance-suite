package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.List;

public class FAPICIBAValidateIdTokenACRClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token",  "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		JsonElement acrValue = env.getElementFromObject("authorization_endpoint_request", "acr_values");
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
				throw error("Invalid acr values in request object", args("actual", acrValue));
			}
		} else {
			logSuccess("Nothing to check; no acr values in request object");
		}
		return env;
	}
}
