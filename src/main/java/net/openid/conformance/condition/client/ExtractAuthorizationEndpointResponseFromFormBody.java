package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthorizationEndpointResponseFromFormBody extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject formBody = (JsonObject) env.getElementFromObject("incoming_request", "body_form_params");
		if (formBody == null) {
			throw error("No form body in incoming request or content-type not application/x-www-form-urlencoded");
		}
		env.putObject("authorization_endpoint_response", formBody);

		logSuccess("Extracted the authorization response", formBody);

		return env;
	}

}
