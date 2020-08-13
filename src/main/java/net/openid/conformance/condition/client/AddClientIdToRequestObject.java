package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

//Adds ClientId to the Request Object
public class AddClientIdToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String clientId = env.getString("client", "client_id");

		if (clientId == null) {
			throw error("missing client_id in environment");
		}

		requestObjectClaims.addProperty("client_id", clientId);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added client_id to request object claims", args("client_id", clientId));

		return env;
	}
}
