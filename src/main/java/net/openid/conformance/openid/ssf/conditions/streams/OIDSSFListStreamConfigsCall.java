package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

/**
 * SSF 1.0 8.1.1.2: a GET on the configuration endpoint without {@code stream_id} returns "a
 * list of the stream configurations available to this Receiver", an empty list when none
 * exist. The response is kept at {@code ssf.stream_list}; unlike {@link OIDSSFReadStreamConfigCall}
 * it leaves {@code ssf.stream} untouched so the module keeps tracking its own stream.
 */
public class OIDSSFListStreamConfigsCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "GET");
		// an error response never reaches handleClientResponse; a list from an earlier read
		// must not survive it
		env.removeElement("ssf", "stream_list");
	}

	@Override
	protected Object getBody(Environment env) {
		return null;
	}

	@Override
	protected String getEndpointName() {
		return "list stream configurations";
	}

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		return getConfigurationEndpointUrl(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonElement bodyJson = env.getElementFromObject("resource_endpoint_response_full", "body_json");
		if (bodyJson != null && bodyJson.isJsonArray()) {
			env.putArray("ssf", "stream_list", bodyJson.getAsJsonArray());
		}
		return env;
	}
}
