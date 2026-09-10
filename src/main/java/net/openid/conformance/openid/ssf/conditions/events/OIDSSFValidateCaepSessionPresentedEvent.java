package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the optional fields of a CAEP Session Presented event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.7.1">CAEP 1.0 Section 3.7.1</a>.
 * All event-specific claims are optional; when present:
 * <ul>
 *   <li>{@code fp_ua} - "Fingerprint of the user agent computed by the Transmitter", a JSON string</li>
 *   <li>{@code ext_id} - "The external session identifier", a JSON string</li>
 * </ul>
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFValidateCaepSessionPresentedEvent extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateOptionalString(eventData, "fp_ua");
		validateOptionalString(eventData, "ext_id");

		logSuccess("Session Presented event fields are valid", args("event_data", eventData));

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
}
