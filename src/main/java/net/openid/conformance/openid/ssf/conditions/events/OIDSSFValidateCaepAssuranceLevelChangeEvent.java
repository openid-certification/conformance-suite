package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates the fields of a CAEP Assurance Level Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.4.1">CAEP 1.0 Section 3.4.1</a>:
 * <ul>
 *   <li>{@code namespace} - "REQUIRED, JSON string: the namespace of the values in the current_level and
 *       previous_level claims"; besides the listed values it may be "any other value that is an alias for a
 *       custom namespace agreed between the Transmitter and the Receiver", so only presence and type are checked</li>
 *   <li>{@code current_level} - "REQUIRED, JSON string: The current assurance level, as defined in the
 *       specified namespace"</li>
 *   <li>{@code previous_level} - "OPTIONAL, JSON string: the previous assurance level"</li>
 *   <li>{@code change_direction} - "OPTIONAL, JSON string: the assurance level increased or decreased ...
 *       If present, this MUST be one of the following strings: increase, decrease"</li>
 * </ul>
 * Reads the event payload from {@code ssf.caep_event.data}. The SHOULD that ties {@code change_direction}
 * to the presence of {@code previous_level} is checked separately by
 * {@link OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing}.
 */
public class OIDSSFValidateCaepAssuranceLevelChangeEvent extends AbstractCondition {

	private static final Set<String> VALID_CHANGE_DIRECTIONS = Set.of("increase", "decrease");

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateRequiredNonEmptyString(eventData, "namespace");
		validateRequiredNonEmptyString(eventData, "current_level");
		validateOptionalString(eventData, "previous_level");
		validateChangeDirection(eventData);

		logSuccess("Assurance Level Change event fields are valid", args("event_data", eventData));

		return env;
	}

	private void validateRequiredNonEmptyString(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			throw error("Missing required field '" + fieldName + "' in assurance-level-change event",
				args("event_data", eventData));
		}
		if (!isString(el)) {
			throw error("Field '" + fieldName + "' MUST be a JSON string",
				args(fieldName, el, "event_data", eventData));
		}
		if (OIDFJSON.getString(el).isEmpty()) {
			throw error("Field '" + fieldName + "' MUST NOT be an empty string",
				args(fieldName, el, "event_data", eventData));
		}
	}

	private void validateOptionalString(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			return;
		}
		if (!isString(el)) {
			throw error("Field '" + fieldName + "' MUST be a JSON string when present",
				args(fieldName, el, "event_data", eventData));
		}
	}

	private void validateChangeDirection(JsonObject eventData) {
		JsonElement el = eventData.get("change_direction");
		if (el == null) {
			return;
		}
		if (!isString(el)) {
			throw error("Field 'change_direction' MUST be a JSON string when present",
				args("change_direction", el, "event_data", eventData));
		}
		String value = OIDFJSON.getString(el);
		if (!VALID_CHANGE_DIRECTIONS.contains(value)) {
			throw error("Field 'change_direction' MUST be one of: increase, decrease",
				args("change_direction", value, "valid_values", VALID_CHANGE_DIRECTIONS, "event_data", eventData));
		}
	}

	private static boolean isString(JsonElement el) {
		return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString();
	}
}
