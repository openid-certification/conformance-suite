package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddBadIssToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.addProperty("iss", "11111111111");
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added bad iss to request object claims", args("iss", requestObjectClaims.getAsJsonPrimitive("iss")));
		return env;
	}
}
