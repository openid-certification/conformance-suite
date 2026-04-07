package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates the common optional CAEP event fields defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-2">CAEP 1.0 Section 2</a>:
 * <ul>
 *   <li>{@code event_timestamp} - MUST be a JSON number (seconds since epoch)</li>
 *   <li>{@code initiating_entity} - MUST be one of: admin, user, policy, system</li>
 *   <li>{@code reason_admin} - MUST be a JSON object (BCP47 language tags as keys)</li>
 *   <li>{@code reason_user} - MUST be a JSON object (BCP47 language tags as keys)</li>
 * </ul>
 */
public class OIDSSFValidateCaepCommonOptionalFields extends AbstractCondition {

	private static final Set<String> VALID_INITIATING_ENTITIES = Set.of("admin", "user", "policy", "system");

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
	}
}
