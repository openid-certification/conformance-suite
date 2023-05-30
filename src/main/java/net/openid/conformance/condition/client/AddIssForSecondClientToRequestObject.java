package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssForSecondClientToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims", "client2" })
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.addProperty("iss", env.getString("client2", "client_id"));
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added bad iss to request object claims", args("iss", requestObjectClaims.getAsJsonPrimitive("iss")));
		return env;
	}
}
