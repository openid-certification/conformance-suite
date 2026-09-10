package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a Stream Updated event as defined in
 * <a href="https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-8.1.5">SSF 1.0 Section 8.1.5</a>.
 * The event {@code https://schemas.openid.net/secevent/ssf/event-type/stream-updated} MUST be present in the
 * {@code events} claim of the SET and:
 * <ul>
 *   <li>{@code status} - "REQUIRED. Defines the new status of the stream." One of {@link SsfConstants.StreamStatus}</li>
 *   <li>{@code reason} - "OPTIONAL. Provides a short description of why the Transmitter has updated the status."</li>
 *   <li>{@code sub_id} - "REQUIRED. The top-level sub_id claim specifies the Stream ID for which the status has been
 *       updated. The value of the sub_id field MUST be of format opaque, and its id value MUST be the unique ID of
 *       the stream."</li>
 * </ul>
 * Reads the parsed SET from {@code set_token.claims} and the expected stream id from {@code ssf.stream.stream_id}.
 * Members of the event object other than {@code status} and {@code reason} are not checked here; see
 * {@link OIDSSFWarnStreamUpdatedEventUnknownMembers}.
 */
public class OIDSSFValidateStreamUpdatedEvent extends AbstractCondition {

	private static final Set<String> VALID_STREAM_STATUSES = Arrays.stream(SsfConstants.StreamStatus.values())
		.map(Enum::name)
		.collect(Collectors.toUnmodifiableSet());

	@PreEnvironment(required = {"set_token", "ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();

		JsonObject event = findStreamUpdatedEvent(claims);
		validateStatus(event);
		validateReason(event);
		validateSubId(env, claims);

		logSuccess("Stream Updated event is valid", args("event", event, "sub_id", claims.get("sub_id")));

		return env;
	}

	private JsonObject findStreamUpdatedEvent(JsonObject claims) {
		JsonElement events = claims.get("events");
		if (events == null || !events.isJsonObject()) {
			throw error("SET does not contain an 'events' claim that is a JSON object", args("claims", claims));
		}
		JsonElement event = events.getAsJsonObject().get(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
		if (event == null) {
			throw error("SET does not contain a stream-updated event",
				args("expected_event_type", SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE,
					"found_event_types", events.getAsJsonObject().keySet()));
		}
		if (!event.isJsonObject()) {
			throw error("The stream-updated event value MUST be a JSON object", args("event", event));
		}
		return event.getAsJsonObject();
	}

	private void validateStatus(JsonObject event) {
		JsonElement status = event.get("status");
		if (status == null) {
			throw error("Missing required field 'status' in stream-updated event", args("event", event));
		}
		if (!isString(status)) {
			throw error("Field 'status' MUST be a JSON string", args("status", status, "event", event));
		}
		String value = OIDFJSON.getString(status);
		if (!VALID_STREAM_STATUSES.contains(value)) {
			throw error("Field 'status' MUST be one of: enabled, paused, disabled",
				args("status", value, "valid_values", VALID_STREAM_STATUSES, "event", event));
		}
	}

	private void validateReason(JsonObject event) {
		JsonElement reason = event.get("reason");
		if (reason == null) {
			return;
		}
		if (!isString(reason)) {
			throw error("Field 'reason' MUST be a JSON string when present", args("reason", reason, "event", event));
		}
	}

	private void validateSubId(Environment env, JsonObject claims) {
		String streamId = env.getString("ssf", "stream.stream_id");
		if (streamId == null) {
			throw error("The id of the current stream is not available in the environment, "
					+ "cannot verify the sub_id of the stream-updated event",
				args("claims", claims));
		}

		JsonElement subIdEl = claims.get("sub_id");
		if (subIdEl == null) {
			throw error("Missing required top-level 'sub_id' claim in stream-updated SET", args("claims", claims));
		}
		if (!subIdEl.isJsonObject()) {
			throw error("The 'sub_id' claim MUST be a JSON object", args("sub_id", subIdEl));
		}
		JsonObject subId = subIdEl.getAsJsonObject();

		JsonElement format = subId.get("format");
		if (format == null || !isString(format)) {
			throw error("The 'sub_id' claim MUST contain a 'format' string", args("sub_id", subId));
		}
		if (!"opaque".equals(OIDFJSON.getString(format))) {
			throw error("The 'sub_id' claim of a stream-updated event MUST be of format 'opaque'",
				args("expected_format", "opaque", "actual_format", OIDFJSON.getString(format), "sub_id", subId));
		}

		JsonElement id = subId.get("id");
		if (id == null || !isString(id)) {
			throw error("The 'sub_id' claim MUST contain an 'id' string", args("sub_id", subId));
		}
		if (!streamId.equals(OIDFJSON.getString(id))) {
			throw error("The 'sub_id.id' of a stream-updated event MUST be the unique ID of the stream",
				args("expected", streamId, "actual", OIDFJSON.getString(id), "sub_id", subId));
		}
	}

	private static boolean isString(JsonElement el) {
		return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString();
	}
}
