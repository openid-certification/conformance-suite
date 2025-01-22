package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureResponseIsJsonArray extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response");
		JsonElement body = endpointResponse.get("body");
		try {
			String bodyString = OIDFJSON.getString(body);
			JsonArray endpointResponseBody = JsonParser.parseString(bodyString).getAsJsonArray();
			if (endpointResponseBody == null || endpointResponseBody.isJsonNull()) {
				throw error("Endpoint response body is not a JSON array", args("endpoint_response_body", body));
			}
			env.putString("endpoint_response_body", bodyString);

			logSuccess("Endpoint response body is a JSON array.");
			return env;
		} catch (JsonSyntaxException | OIDFJSON.UnexpectedJsonTypeException e) {
			throw error("Failed to parse endpoint response body JSON", args("body", body));
		}
	}

}
