package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectBindingMessageToTooShortPurpose extends AbstractCondition {

	public static final String TOO_SHORT_PURPOSE = "no";

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		requestObjectClaims.addProperty("binding_message", TOO_SHORT_PURPOSE);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Set binding_message in request object claims to a value that is too short",
			args("binding_message", TOO_SHORT_PURPOSE, "request_object_claims", requestObjectClaims));

		return env;
	}
}
