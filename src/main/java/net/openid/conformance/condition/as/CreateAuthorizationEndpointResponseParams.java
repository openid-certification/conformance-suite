package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class CreateAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_response_params")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("authorization_endpoint_request", "params.redirect_uri");
		String state = env.getString("authorization_endpoint_request", "params.state");

		JsonObject responseParams = new JsonObject();
		responseParams.addProperty("redirect_uri", redirectUri);
		if(state!=null) {
			responseParams.addProperty("state", state);
		}

		logSuccess("Added authorization_endpoint_response_params to environment", args("params", responseParams));

		env.putObject("authorization_endpoint_response_params", responseParams);

		return env;

	}

}
