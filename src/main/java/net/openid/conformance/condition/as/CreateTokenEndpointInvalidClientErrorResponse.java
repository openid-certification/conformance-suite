package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates an OAuth 2.0 error response for the token endpoint when client authentication fails.
 * Returns HTTP 401 with the specified error code (default: invalid_client) per RFC 6749 / OAuth2-ATCA07.
 */
public class CreateTokenEndpointInvalidClientErrorResponse extends AbstractCondition {

	private final String errorCode;

	public CreateTokenEndpointInvalidClientErrorResponse() {
		this.errorCode = "invalid_client";
	}

	public CreateTokenEndpointInvalidClientErrorResponse(String errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	@PostEnvironment(required = {"token_endpoint_error_response"})
	public Environment evaluate(Environment env) {
		String errorDescription = env.getString("token_endpoint_client_auth_error_description");
		if (errorDescription == null) {
			errorDescription = "Client authentication failed";
		}

		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("error", errorCode);
		endpointResponse.addProperty("error_description", errorDescription);

		env.putObject("token_endpoint_error_response", endpointResponse);
		env.putInteger("token_endpoint_error_response_http_status", 401);

		logSuccess("Created token endpoint " + errorCode + " error response",
			args("error", errorCode, "error_description", errorDescription));

		return env;
	}
}
