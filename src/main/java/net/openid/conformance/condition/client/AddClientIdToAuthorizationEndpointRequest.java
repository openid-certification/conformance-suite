package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientIdToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client" } )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String clientId = env.getString("client", "client_id");
		if (Strings.isNullOrEmpty(clientId)) {
			throw error("client_id missing/empty in client object");
		}

		authorizationEndpointRequest.addProperty("client_id", clientId);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added client_id of '"+clientId+"' to authorization endpoint request", authorizationEndpointRequest);

		return env;
	}

}
