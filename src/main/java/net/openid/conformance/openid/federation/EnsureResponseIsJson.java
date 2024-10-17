package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;

public class EnsureResponseIsJson extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(strings = "endpoint_response_body_string")
	@PostEnvironment(required = "endpoint_response_body")
	public Environment evaluate(Environment env) {

		String body = env.getString("endpoint_response_body_string");
		try {
			JsonObject endpointResponseBody = JsonParser.parseString(body).getAsJsonObject();
			env.putObject("endpoint_response_body", endpointResponseBody);
			logSuccess("Endpoint response is JSON");
			return env;
		} catch (JsonSyntaxException e) {
			throw error("Endpoint response is not JSON", args("endpoint_response_body", body));
		}

	}
}
