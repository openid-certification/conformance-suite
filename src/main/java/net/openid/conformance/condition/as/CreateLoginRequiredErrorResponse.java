package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CreateLoginRequiredErrorResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response_params")
	@PostEnvironment(required = "error_response_params", strings = "error_response_url")
	public Environment evaluate(Environment env) {
		JsonObject originalResponseParams = env.getObject("authorization_endpoint_response_params");
		JsonObject errorResponseParams = new JsonObject();
		if(originalResponseParams.has("state")) {
			errorResponseParams.add("state", originalResponseParams.get("state"));
		}
		errorResponseParams.addProperty("error", "login_required");
		errorResponseParams.addProperty("error_description", "This is a login_required error response");
		env.putObject("error_response_params", errorResponseParams);

		String responseUrl = OIDFJSON.getString(originalResponseParams.remove("redirect_uri"));
		env.putString("error_response_url", responseUrl);

		logSuccess("Created login_required error",
					args("error_response_params", errorResponseParams, "error_response_url", responseUrl));

		return env;

	}

}
