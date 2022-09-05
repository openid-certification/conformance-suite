package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthorizationEndpointRequestFromClientInformation extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client", strings = "redirect_uri" )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id");

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("Couldn't find client ID");
		}

		String redirectUri = env.getString("redirect_uri");

		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("Couldn't find redirect URI");
		}

		JsonObject authorizationEndpointRequest = new JsonObject();

		authorizationEndpointRequest.addProperty("client_id", clientId);
		authorizationEndpointRequest.addProperty("redirect_uri", redirectUri);

		String scope = env.getString("client", "scope");
		if (!Strings.isNullOrEmpty(scope)) {
			authorizationEndpointRequest.addProperty("scope", scope);
		} else {
			log("Leaving off 'scope' parameter from authorization request");
		}

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Created authorization endpoint request", authorizationEndpointRequest);

		return env;

	}

}
