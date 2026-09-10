package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the optional fields of a CAEP Session Established event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.6.1">CAEP 1.0 Section 3.6.1</a>.
 * All event-specific claims are optional; when present:
 * <ul>
 *   <li>{@code fp_ua} - "Fingerprint of the user agent computed by the Transmitter", a JSON string</li>
 *   <li>{@code acr} - "The authentication context class reference of the session ... MUST be interpreted in the
 *       same way as the corresponding field in an OpenID Connect ID Token", a JSON string</li>
 *   <li>{@code amr} - "The value of this field MUST be an array of strings"</li>
 *   <li>{@code ext_id} - "The external session identifier", a JSON string</li>
 * </ul>
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFValidateCaepSessionEstablishedEvent extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateOptionalString(eventData, "fp_ua");
		validateOptionalString(eventData, "acr");
		validateOptionalStringArray(eventData, "amr");
		validateOptionalString(eventData, "ext_id");

		logSuccess("Session Established event fields are valid", args("event_data", eventData));

		return env;
	}

	private void validateOptionalString(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field '" + fieldName + "' MUST be a JSON string when present",
				args(fieldName, el, "event_data", eventData));
		}
	}

	private void validateOptionalStringArray(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			return;
		}
		if (!el.isJsonArray()) {
			throw error("Field '" + fieldName + "' MUST be an array of strings when present",
				args(fieldName, el, "event_data", eventData));
		}
		JsonArray array = el.getAsJsonArray();
		for (JsonElement item : array) {
			if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
				throw error("Field '" + fieldName + "' MUST contain only JSON strings",
					args(fieldName, array, "invalid_item", item, "event_data", eventData));
			}
		}
	}
}
