package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.AbstractOIDSSFTransmitterEndpointCall;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class OIDSSFTriggerVerificationEvent extends AbstractOIDSSFTransmitterEndpointCall {

	@Override
	protected String getEndpointName() {
		return "transmitter_endpoint";
	}

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		return getVerificationEndpointUrl(env);
	}

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "POST");

		String streamId = env.getString("ssf", "stream.stream_id");
		String state = UUID.randomUUID().toString();

		env.putString("ssf", "verification.state", state);

		env.putString("resource_request_entity",
			new Gson().toJson(
				Map.of(
					"stream_id", streamId,
					"state", state
				)
			));

		// Record the moment this verification request is sent so a subsequent
		// trigger can honor the transmitter's advertised min_verification_interval
		// (SSF 1.0 7.1.1.1) via OIDSSFWaitForMinVerificationInterval.
		env.putString("ssf", "last_verification_trigger_at", Instant.now().toString());
	}

	@Override
	protected boolean requireJsonResponseBody() {
		return false;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		logSuccess("Got a response from the verification endpoint",
			args("body", responseBody, "headers", responseHeaders, "status_code", responseCode, "state", env.getString("ssf", "verification.state")));
		return env;
	}
}
