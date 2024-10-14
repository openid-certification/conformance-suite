package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFReadStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "GET");
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		env.putString("stream_id", env.getString("resource_endpoint_response_full","body_json.stream_id"));
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
