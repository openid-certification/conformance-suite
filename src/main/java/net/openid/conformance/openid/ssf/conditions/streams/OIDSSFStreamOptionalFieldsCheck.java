package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the types of the optional members of a stream configuration (SSF 1.0 8.1.1):
 * {@code events_supported} and {@code events_requested} are arrays of strings,
 * {@code min_verification_interval} and {@code inactivity_timeout} are non-negative integer
 * numbers of seconds, and {@code description} is a string. Reads {@code ssf.stream}.
 */
public class OIDSSFStreamOptionalFieldsCheck extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		if (streamEl == null || !streamEl.isJsonObject()) {
			throw error("ssf stream element not found");
		}

		JsonObject stream = streamEl.getAsJsonObject();

		List<String> present = new ArrayList<>();
		checkStringArray(stream, "events_supported", present);
		checkStringArray(stream, "events_requested", present);
		checkNonNegativeInteger(stream, "min_verification_interval", present);
		checkNonNegativeInteger(stream, "inactivity_timeout", present);
		checkString(stream, "description", present);

		if (present.isEmpty()) {
			log("Found no optional fields in stream configuration");
		} else {
			logSuccess("Optional fields in the stream configuration have the expected types", args("optional_fields", present));
		}

		return env;
	}

	private void checkStringArray(JsonObject stream, String field, List<String> present) {
		if (!stream.has(field)) {
			return;
		}
		present.add(field);
		JsonElement el = stream.get(field);
		if (!el.isJsonArray()) {
			throw error(field + " in the stream configuration must be a JSON array of event type URIs", args(field, el));
		}
		for (JsonElement member : el.getAsJsonArray()) {
			if (!member.isJsonPrimitive() || !member.getAsJsonPrimitive().isString() || OIDFJSON.getString(member).isBlank()) {
				throw error(field + " in the stream configuration must only contain non-empty strings", args(field, el, "member", member));
			}
		}
	}

	private void checkNonNegativeInteger(JsonObject stream, String field, List<String> present) {
		if (!stream.has(field)) {
			return;
		}
		present.add(field);
		JsonElement el = stream.get(field);
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
			throw error(field + " in the stream configuration must be a JSON number (seconds)", args(field, el));
		}
		double value = OIDFJSON.getDouble(el);
		if (value < 0 || value != Math.floor(value)) {
			throw error(field + " in the stream configuration must be a non-negative integer number of seconds", args(field, el));
		}
	}

	private void checkString(JsonObject stream, String field, List<String> present) {
		if (!stream.has(field)) {
			return;
		}
		present.add(field);
		JsonElement el = stream.get(field);
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error(field + " in the stream configuration must be a JSON string", args(field, el));
		}
	}
}
