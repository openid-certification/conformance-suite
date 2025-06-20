package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractVP1FinalBrowserApiResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "original_authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement body = env.getElementFromObject("incoming_request", "body");
		// as the http request we're processing here is sent by our own javascript code in log-detail.html
		// these really should never fail
		if (body == null) {
			throw error("No body received in browser API submission");
		}
		if (!body.isJsonPrimitive() || !body.getAsJsonPrimitive().isString()) {
			throw error("Body received in browser API submission is not a string", args("body", body));
		}

		JsonObject result;
		try {
			result = JsonParser.parseString(OIDFJSON.getString(body)).getAsJsonObject();
		} catch (JsonParseException e) {
			throw error("Parsing JSON in browser API submission failed", e);
		}

		// The format of the 'result' JSON object is defined by log-detail.html
		if (result.has("bad_response_type")) {
			throw error("Browser API returned object of unknown type", args("bad_response_type", result.get("bad_response_type")));
		}

		if (result.has("exception")) {
			throw error("Browser API threw an exception", args("exception", result.get("exception")));
		}

		JsonElement protocolEl = result.get("protocol");
		if (!protocolEl.isJsonPrimitive() || !protocolEl.getAsJsonPrimitive().isString()) {
			throw error("Protocol returned by browser API is not a string", args("protocol", protocolEl));
		}
		String protocol = OIDFJSON.getString(protocolEl);
		JsonArray requests = env.getElementFromObject("browser_api_request", "digital.requests").getAsJsonArray();
		String expectedProtocol = OIDFJSON.getString(requests.get(0).getAsJsonObject().get("protocol"));
		if (!protocol.equals(expectedProtocol)) {
			throw error("Protocol returned by browser API is not the requested one", args("protocol", protocol, "expected_protocol", expectedProtocol));
		}

		JsonElement dataEl = result.get("data");
		if (!dataEl.isJsonObject()) {
			throw error("'data' member returned by browser API is not a JSON object", args("response", result));
		}

		JsonObject data = dataEl.getAsJsonObject();
		env.putObject("original_authorization_endpoint_response", data);

		logSuccess("Browser API result captured and successfully parsed", args("api_result", result, "parsed_data", data));

		return env;
	}

}
