package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectBindingMessageToNonAsciiPurpose extends AbstractCondition {

	public static final String NON_ASCII_PURPOSE = "Confirm sharing your identité details";

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		requestObjectClaims.addProperty("binding_message", NON_ASCII_PURPOSE);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Set binding_message in request object claims to a non-ASCII purpose",
			args("binding_message", NON_ASCII_PURPOSE, "request_object_claims", requestObjectClaims));

		return env;
	}
}
