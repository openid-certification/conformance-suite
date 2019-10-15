package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String clientId = env.getString("client", "client_id");

		if (clientId != null) {

			requestObjectClaims.addProperty("iss", clientId);

			env.putObject("request_object_claims", requestObjectClaims);

			logSuccess("Added iss to request object claims", args("iss", clientId));

		} else {

			// Only a "should" requirement
			log("Request object contains no issuer and client ID not found");

		}

		return env;
	}
}
