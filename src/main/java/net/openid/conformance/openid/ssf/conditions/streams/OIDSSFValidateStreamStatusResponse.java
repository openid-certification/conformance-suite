package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.List;

/**
 * Validates a stream status document (SSF 1.0 8.1.2.1, also returned by a status update per
 * 8.1.2.2): {@code stream_id} is a string identifying the stream whose status was queried,
 * {@code status} "MUST be one of" enabled, paused, disabled, and {@code reason} is an
 * OPTIONAL string. The status is kept at {@code ssf.stream_status} for later checks.
 * <p>
 * Expects the response under {@code endpoint_response} (map {@code resource_endpoint_response_full}
 * onto it first).
 */
public class OIDSSFValidateStreamStatusResponse extends AbstractCondition {

	private static final List<String> VALID_STATUS_VALUES = Arrays.stream(StreamStatus.values()).map(Enum::name).toList();

	@Override
	@PreEnvironment(required = {"endpoint_response", "ssf"})
	public Environment evaluate(Environment env) {

		JsonElement bodyEl = env.getElementFromObject("endpoint_response", "body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			throw error("The stream status response is not a JSON object",
				args("body", env.getElementFromObject("endpoint_response", "body")));
		}
		JsonObject status = bodyEl.getAsJsonObject();

		JsonElement streamIdEl = status.get("stream_id");
		if (streamIdEl == null) {
			throw error("The stream status response does not contain 'stream_id'", args("status", status));
		}
		if (!isString(streamIdEl)) {
			throw error("'stream_id' in the stream status response must be a JSON string", args("stream_id", streamIdEl));
		}
		String expectedStreamId = env.getString("ssf", "stream.stream_id");
		if (expectedStreamId != null && !expectedStreamId.equals(OIDFJSON.getString(streamIdEl))) {
			throw error("The stream status response identifies a different stream than the one whose status was requested",
				args("expected_stream_id", expectedStreamId, "stream_id", streamIdEl));
		}

		JsonElement statusEl = status.get("status");
		if (statusEl == null) {
			throw error("The stream status response does not contain 'status'", args("status", status));
		}
		if (!isString(statusEl) || !VALID_STATUS_VALUES.contains(OIDFJSON.getString(statusEl))) {
			throw error("'status' in the stream status response must be one of " + VALID_STATUS_VALUES,
				args("status", statusEl, "valid_values", VALID_STATUS_VALUES));
		}

		JsonElement reasonEl = status.get("reason");
		if (reasonEl != null && !isString(reasonEl)) {
			throw error("'reason' in the stream status response must be a JSON string when present",
				args("reason", reasonEl));
		}

		env.putObject("ssf", "stream_status", status);

		logSuccess("The stream status response is valid", args("stream_status", status));

		return env;
	}

	private static boolean isString(JsonElement el) {
		return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString();
	}
}
