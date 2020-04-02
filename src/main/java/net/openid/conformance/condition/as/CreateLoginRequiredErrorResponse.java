package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CreateLoginRequiredErrorResponse extends AbstractCondition {

	public static final String ERROR_RESPONSE_PARAMS = "error_response_params";
	public static final String ERROR_RESPONSE_URL = "error_response_url";

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = ERROR_RESPONSE_PARAMS, strings = ERROR_RESPONSE_URL)
	public Environment evaluate(Environment env) {
		JsonObject originalResponseParams = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);
		JsonObject errorResponseParams = new JsonObject();
		if(originalResponseParams.has("state")) {
			errorResponseParams.add("state", originalResponseParams.get("state"));
		}
		errorResponseParams.addProperty("error", "login_required");
		errorResponseParams.addProperty("error_description", "This is a login_required error response");
		env.putObject(ERROR_RESPONSE_PARAMS, errorResponseParams);

		String responseUrl = OIDFJSON.getString(originalResponseParams.remove("redirect_uri"));
		env.putString(ERROR_RESPONSE_URL, responseUrl);

		log("Created login_required error",
					args(ERROR_RESPONSE_PARAMS, errorResponseParams, ERROR_RESPONSE_URL, responseUrl));

		return env;

	}

}
