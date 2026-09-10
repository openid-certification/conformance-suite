package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates the common optional CAEP event fields defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-2">CAEP 1.0 Section 2</a>:
 * <ul>
 *   <li>{@code event_timestamp} - MUST be a JSON number of seconds since the epoch (a value in
 *       milliseconds, or a negative or fractional value, violates the definition)</li>
 *   <li>{@code initiating_entity} - MUST be one of: admin, user, policy, system</li>
 *   <li>{@code reason_admin} - MUST be a JSON object whose keys are BCP 47 language tags</li>
 *   <li>{@code reason_user} - MUST be a JSON object whose keys are BCP 47 language tags</li>
 * </ul>
 */
public class OIDSSFValidateCaepCommonOptionalFields extends AbstractCondition {

	private static final Set<String> VALID_INITIATING_ENTITIES = Set.of("admin", "user", "policy", "system");

	/**
	 * Seconds-since-epoch values at or above this are in the year 5138, i.e. the transmitter
	 * used milliseconds.
	 */
	static final long MAX_PLAUSIBLE_EPOCH_SECONDS = 100_000_000_000L;

	/**
	 * Language tags per RFC 5646 section 2.1: langtag, private-use, and the irregular
	 * grandfathered forms. The primary language subtag is limited to the two- and three-letter
	 * ISO 639 codes; the 4- to 8-letter forms the grammar reserves have no registered values,
	 * and admitting them would let plain words such as "message" pass as language tags.
	 */
	private static final Pattern LANGUAGE_TAG = Pattern.compile(
		"^(?:[A-Za-z]{2,3}(?:-[A-Za-z]{3}){0,3}"
			+ "(?:-[A-Za-z]{4})?"
			+ "(?:-(?:[A-Za-z]{2}|[0-9]{3}))?"
			+ "(?:-(?:[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*"
			+ "(?:-[0-9A-WY-Za-wy-z](?:-[A-Za-z0-9]{2,8})+)*"
			+ "(?:-[Xx](?:-[A-Za-z0-9]{1,8})+)?"
			+ "|[Xx](?:-[A-Za-z0-9]{1,8})+"
			+ "|en-GB-oed|i-(?:ami|bnn|default|enochian|hak|klingon|lux|mingo|navajo|pwn|tao|tay|tsu)|sgn-(?:BE-FR|BE-NL|CH-DE))$");

	static boolean isWellFormedLanguageTag(String tag) {
		return LANGUAGE_TAG.matcher(tag).matches();
	}

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();
		String eventType = env.getString("ssf", "caep_event.type");

		validateEventTimestamp(eventData, eventType);
		validateInitiatingEntity(eventData, eventType);
		validateReasonObject(eventData, "reason_admin", eventType);
		validateReasonObject(eventData, "reason_user", eventType);

		logSuccess("Common CAEP optional fields are valid", args("event_type", eventType));

		return env;
	}

	private void validateEventTimestamp(JsonObject eventData, String eventType) {
		JsonElement el = eventData.get("event_timestamp");
		if (el == null) {
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
			throw error("event_timestamp MUST be a JSON number (seconds since Unix epoch)",
				args("event_type", eventType, "event_timestamp", el));
		}
		double value = OIDFJSON.getDouble(el);
		if (value < 0 || value != Math.floor(value)) {
			throw error("event_timestamp must be a non-negative whole number of seconds since the Unix epoch",
				args("event_type", eventType, "event_timestamp", el));
		}
		if (value >= MAX_PLAUSIBLE_EPOCH_SECONDS) {
			throw error("event_timestamp is far beyond any plausible date when read as seconds; it appears to be in milliseconds, "
					+ "but the value is defined as the number of seconds since the Unix epoch",
				args("event_type", eventType, "event_timestamp", el));
		}
	}

	private void validateInitiatingEntity(JsonObject eventData, String eventType) {
		JsonElement el = eventData.get("initiating_entity");
		if (el == null) {
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("initiating_entity MUST be a JSON string",
				args("event_type", eventType, "initiating_entity", el));
		}
		String value = OIDFJSON.getString(el);
		if (!VALID_INITIATING_ENTITIES.contains(value)) {
			throw error("initiating_entity MUST be one of: admin, user, policy, system",
				args("event_type", eventType, "initiating_entity", value, "valid_values", VALID_INITIATING_ENTITIES));
		}
	}

	private void validateReasonObject(JsonObject eventData, String fieldName, String eventType) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			return;
		}
		if (!el.isJsonObject()) {
			throw error(fieldName + " MUST be a JSON object with BCP47 language tags as keys",
				args("event_type", eventType, fieldName, el));
		}
		JsonObject reasonObj = el.getAsJsonObject();
		if (reasonObj.isEmpty()) {
			throw error(fieldName + " MUST contain at least one BCP47-tagged message",
				args("event_type", eventType, fieldName, reasonObj));
		}
		for (Map.Entry<String, JsonElement> entry : reasonObj.entrySet()) {
			if (!isWellFormedLanguageTag(entry.getKey())) {
				throw error(fieldName + " keys MUST be BCP 47 language tags (for example 'en' or 'de-CH')",
					args("event_type", eventType, "key", entry.getKey(), fieldName, reasonObj));
			}
			if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
				throw error(fieldName + " values MUST be strings (localized messages)",
					args("event_type", eventType, "key", entry.getKey(), "value", entry.getValue(), fieldName, reasonObj));
			}
		}
	}
}
