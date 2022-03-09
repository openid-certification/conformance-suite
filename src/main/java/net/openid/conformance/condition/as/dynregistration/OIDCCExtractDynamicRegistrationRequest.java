package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDCCExtractDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "incoming_request"})
	@PostEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		String requestBody = OIDFJSON.getString(env.getObject("incoming_request").get("body"));
		JsonObject requestJson = JsonParser.parseString(requestBody).getAsJsonObject();
		env.putObject("dynamic_registration_request", requestJson);
		logSuccess("Extracted dynamic client registration request", args("request", requestJson));
		return env;
	}
}
