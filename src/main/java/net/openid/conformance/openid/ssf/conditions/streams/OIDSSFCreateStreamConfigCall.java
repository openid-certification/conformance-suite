package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFCreateStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");
		addResourceRequestEntity(env);
	}

	protected void addResourceRequestEntity(Environment env) {
		env.putString("resource_request_entity", createResourceRequestEntityString(env));
	}

	protected String createResourceRequestEntityString(Environment env) {
		return getResourceRequestEntityStringWithOverride(env);
	}

	private String getResourceRequestEntityStringWithOverride(Environment env) {
		String override = env.getString("ssf", "stream.config_override_json");
		if (override != null) {
			return override;
		}
		return new Gson().toJson(getResourceRequestEntityWithOverride(env));
	}

	private JsonObject getResourceRequestEntityWithOverride(Environment env) {
		JsonElement streamConfigOverride = env.getElementFromObject("ssf", "stream.config_override");
		if (streamConfigOverride != null) {
			return streamConfigOverride.getAsJsonObject();
		}
		return createResourceRequestEntity(env);
	}

	protected JsonObject createResourceRequestEntity(Environment env) {
		return env.getElementFromObject("ssf", "stream.config").getAsJsonObject();
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonObject streamConfigObject = env.getElementFromObject("resource_endpoint_response_full", "body_json").getAsJsonObject();
		env.putObject("ssf", "stream", streamConfigObject);
		return env;
	}
}
