package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * PAR-2.0 : This class tries to set the PAR endpoint URL as the Audience for PAR request.
 */
public class AddPAREndpointAsAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"request_object_claims"})
	@PostEnvironment(required = {"request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject authzEndpointRequest = env.getObject("request_object_claims");

		String parEndpoint = env.getString("server", "pushed_authorization_request_endpoint");

		if (parEndpoint == null) {
			throw error("Could not find pushed_authorization_request_endpoint in the server metadata.");
		}

		authzEndpointRequest.addProperty("aud", parEndpoint);

		env.putObject("request_object_claims", authzEndpointRequest);

		logSuccess("Added aud to request object claims", args("aud", parEndpoint));

		return env;
	}
}
