package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

		// Negative tests (SSF 1.0 8.1.4.2, "400 if the request body cannot be parsed") send
		// the override verbatim instead of a verification request.
		String bodyOverride = env.getString("ssf", "verification.request_body_override");
		if (bodyOverride != null) {
			env.putString("resource_request_entity", bodyOverride);
			log("Sending the verification request body override", args("request_body", bodyOverride));
		} else {
			String streamId = env.getString("ssf", "stream.stream_id");
			String state = UUID.randomUUID().toString();

			// The latest state is what the current wait loop looks for; the full list lets the
			// state check recognise a late echo of an earlier request as legitimate (SSF 1.0
			// 8.1.4.2: verification events need not arrive in order).
			env.putString("ssf", "verification.state", state);
			JsonElement issuedStatesEl = env.getElementFromObject("ssf", "verification.issued_states");
			JsonArray issuedStates = issuedStatesEl != null && issuedStatesEl.isJsonArray()
				? issuedStatesEl.getAsJsonArray() : new JsonArray();
			issuedStates.add(state);
			env.putArray("ssf", "verification.issued_states", issuedStates);

			env.putString("resource_request_entity",
				new Gson().toJson(
					Map.of(
						"stream_id", streamId,
						"state", state
					)
				));
		}

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
