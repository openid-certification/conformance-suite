package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates an OAuth 2.0 error response for the PAR endpoint when client authentication fails.
 * Returns HTTP 401 with error=invalid_client per RFC 6749.
 */
public class CreatePAREndpointInvalidClientErrorResponse extends AbstractCondition {

	@Override
	@PostEnvironment(required = {"par_endpoint_response"})
	public Environment evaluate(Environment env) {
		String errorDescription = env.getString("par_endpoint_client_auth_error_description");
		if (errorDescription == null) {
			errorDescription = "Client authentication failed";
		}

		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("error", "invalid_client");
		endpointResponse.addProperty("error_description", errorDescription);

		env.putObject("par_endpoint_response", endpointResponse);
		env.putInteger("par_endpoint_response_http_status", 401);

		logSuccess("Created PAR endpoint invalid_client error response",
			args("error", "invalid_client", "error_description", errorDescription));

		return env;
	}
}
