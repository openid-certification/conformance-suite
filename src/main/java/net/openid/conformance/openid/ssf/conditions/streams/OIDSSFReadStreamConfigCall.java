package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFReadStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "GET");
	}

	@Override
	protected Object getBody(Environment env) {
		return null;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonElement endpointResponseFullJsonElement = env.getElementFromObject("resource_endpoint_response_full", "body_json");
		if (endpointResponseFullJsonElement.isJsonObject()) {
			env.putObject("ssf", "stream", endpointResponseFullJsonElement.getAsJsonObject());
		} else if (endpointResponseFullJsonElement.isJsonArray()) {
			env.putArray("ssf", "streams", endpointResponseFullJsonElement.getAsJsonArray());

			if (!endpointResponseFullJsonElement.getAsJsonArray().isEmpty()) {
				env.putObject("ssf", "stream", endpointResponseFullJsonElement.getAsJsonArray().get(0).getAsJsonObject());
			}
		}
		return env;
	}

	@Override
	protected String getEndpointName() {
		return "read stream configuration";
	}

	@Override
	protected void configureResourceUrl(Environment env) {
		String readStreamUri = getStreamConfigEndpointUrlWithStreamIdIfPresent(env);
		env.putString("protected_resource_url", readStreamUri);
	}
}
