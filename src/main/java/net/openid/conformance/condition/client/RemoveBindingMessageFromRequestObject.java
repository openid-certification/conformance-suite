package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveBindingMessageFromRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		requestObjectClaims.remove("binding_message");

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Removed binding_message from request object claims", args("request_object_claims", requestObjectClaims));

		return env;
	}
}
