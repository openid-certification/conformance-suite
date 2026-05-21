package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Sets the Authzen API request body to a verbatim copy of the supplied JSON object,
 * bypassing the field-stripping and required-property checks performed by the
 * Create*Steps sequences. Intended for negative tests that need to deliver an
 * intentionally malformed payload to the PDP.
 */
public class CreateAuthzenApiEndpointRequestFromRaw extends AbstractCondition {

	private final JsonObject rawRequest;

	public CreateAuthzenApiEndpointRequestFromRaw(JsonObject rawRequest) {
		this.rawRequest = rawRequest;
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		env.putObject("authzen_api_endpoint_request", rawRequest.deepCopy());
		env.removeObject("authzen_api_endpoint_request_headers");
		logSuccess("Set Authzen API request to raw payload", rawRequest);
		return env;
	}
}
