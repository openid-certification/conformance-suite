package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.AbstractOIDSSFTransmitterEndpointCall;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class OIDSSFCallPollEndpoint extends AbstractOIDSSFTransmitterEndpointCall {

	@Override
	protected String getEndpointName() {
		return "Poll Endpoint";
	}

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		String pollEndpoint = OIDFJSON.getString(env.getElementFromObject("ssf", "stream.delivery.endpoint_url"));
		return pollEndpoint;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		Environment environment = super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		return environment;
	}

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");

		// See: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.2
		Map<Object, Object> pollRequest = Map.of( //
			"maxEvents", 10,  // retrieve 10 events at max
			"returnImmediately", true, // no long polling
			"ack", List.of(), // we acknowledge no items
			"setErrs", Map.of() //
		);

		String json = new Gson().toJson(pollRequest);
		env.putString("resource_request_entity", json);
	}
}
