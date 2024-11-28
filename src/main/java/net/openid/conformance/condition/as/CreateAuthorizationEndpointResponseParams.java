package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthorizationEndpointResponseParams extends AbstractCondition {

	public static final String ENV_KEY = "authorization_endpoint_response_params";
	public static final String REDIRECT_URI = "redirect_uri";
	public static final String STATE = "state";

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI);

		String state = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);

		JsonObject responseParams = new JsonObject();
		if (redirectUri != null) {
			responseParams.addProperty(REDIRECT_URI, redirectUri);
		}
		if(state!=null) {
			responseParams.addProperty(STATE, state);
		}

		logSuccess("Added "+ENV_KEY+" to environment", args("params", responseParams));

		env.putObject(ENV_KEY, responseParams);

		return env;

	}

}
