package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates the required fields for a CAEP Credential Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.3.1">CAEP 1.0 Section 3.3.1</a>:
 * <ul>
 *   <li>{@code credential_type} - "REQUIRED, JSON string: This MUST be one of the following strings, or any other
 *       credential type supported mutually by the Transmitter and the Receiver." Because extension values are
 *       permitted, only presence and type are checked here; non-standard values are flagged separately by
 *       {@link OIDSSFWarnNonStandardCaepCredentialChangeValues}.</li>
 *   <li>{@code change_type} - "REQUIRED, JSON string: This MUST be one of the following strings:
 *       create, revoke, update, delete". This is a closed set, so any other value fails.</li>
 * </ul>
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFValidateCaepCredentialChangeEvent extends AbstractCondition {

	private static final Set<String> VALID_CHANGE_TYPES = Set.of("create", "revoke", "update", "delete");

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateRequiredStringField(eventData, "credential_type");
		JsonElement changeType = validateRequiredStringField(eventData, "change_type");

		String changeTypeValue = OIDFJSON.getString(changeType);
		if (!VALID_CHANGE_TYPES.contains(changeTypeValue)) {
			throw error("Field 'change_type' MUST be one of: create, revoke, update, delete",
				args("change_type", changeTypeValue, "valid_values", VALID_CHANGE_TYPES, "event_data", eventData));
		}

		logSuccess("Credential Change event fields are valid", args("event_data", eventData));

		return env;
	}

	private JsonElement validateRequiredStringField(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			throw error("Missing required field '" + fieldName + "' in credential-change event",
				args("event_data", eventData));
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field '" + fieldName + "' MUST be a JSON string",
				args(fieldName, el, "event_data", eventData));
		}
		return el;
	}
}
