package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * PAR-2.0 : This class tries to set the PAR endpoint URL as the Audience for PAR request.
 * and if PAR is null then tries to set token_endpoint URL as the Audience.
 */
public class AddPAREndpointAsAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	@PostEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject authzEndpointRequest = env.getObject("authorization_endpoint_request");

		String serverIssuerUrl = env.getString("server", "pushed_authorization_request_endpoint");

		if (serverIssuerUrl == null) {
			serverIssuerUrl = env.getString("server", "token_endpoint");
		}

		if (serverIssuerUrl == null) {
			throw error("Could not set audience in request object as issuer URL is not found in environment");
		}

		authzEndpointRequest.addProperty("aud", serverIssuerUrl);

		env.putObject("authorization_endpoint_request", authzEndpointRequest);

		logSuccess("Added aud to authorization endpoint request", args("aud", serverIssuerUrl));

		return env;
	}
}
