package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureResponseIsJsonObject extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "endpoint_response")
	@PostEnvironment(required = "endpoint_response_body")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response");
		JsonElement body = endpointResponse.get("body");
		try {
			String bodyString = OIDFJSON.getString(body);
			JsonObject endpointResponseBody = JsonParser.parseString(bodyString).getAsJsonObject();
			env.putObject("endpoint_response_body", endpointResponseBody);
			logSuccess("Endpoint response is JSON");
			return env;
		} catch (JsonSyntaxException | OIDFJSON.UnexpectedJsonTypeException e) {
			throw error("Endpoint response is not a valid JSON object", args("endpoint_response_body", body));
		}

	}
}
