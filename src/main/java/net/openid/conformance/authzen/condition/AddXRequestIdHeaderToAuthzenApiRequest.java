package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

/**
 * Adds an `X-Request-ID` header to the Authzen API request and records the
 * value sent under the env key `authzen_api_endpoint_request_x_request_id`
 * for later assertion against the response.
 */
public class AddXRequestIdHeaderToAuthzenApiRequest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "authzen_api_endpoint_request_x_request_id")
	public Environment evaluate(Environment env) {
		String requestId = "conformance-" + UUID.randomUUID();

		JsonObject headers = env.getObject("authzen_api_endpoint_request_headers");
		if (headers == null) {
			headers = new JsonObject();
			env.putObject("authzen_api_endpoint_request_headers", headers);
		}
		headers.addProperty("X-Request-ID", requestId);
		env.putString("authzen_api_endpoint_request_x_request_id", requestId);

		logSuccess("Added X-Request-ID header to Authzen API request",
			args("X-Request-ID", requestId));
		return env;
	}
}
