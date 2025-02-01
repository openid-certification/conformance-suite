package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateFormBodyContainsOnlyResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonObject formBody = (JsonObject) env.getElementFromObject("incoming_request", "body_form_params");
		if (formBody == null) {
			throw error("No form body in incoming request or content-type not application/x-www-form-urlencoded");
		}

		if (!formBody.has("response")) {
			throw error("'response' parameter missing from incoming response", args("form_params", formBody));
		}

		if (formBody.size() != 1) {
			throw error("Incoming response contains parameters other than 'response'", args("form_params", formBody));
		}

		logSuccess("Incoming response contains only expected 'response' parameter", formBody);

		return env;
	}

}
