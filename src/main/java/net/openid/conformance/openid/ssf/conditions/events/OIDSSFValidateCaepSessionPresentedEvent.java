package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the optional fields of a CAEP Session Presented event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.7.1">CAEP 1.0 Section 3.7.1</a>.
 * All event-specific claims are optional; when present:
 * <ul>
 *   <li>{@code fp_ua} - "Fingerprint of the user agent computed by the Transmitter"; no type is
 *       defined, a non-string value is noted</li>
 *   <li>{@code ext_id} - "The external session identifier"; no type is defined, a non-string
 *       value is noted</li>
 * </ul>
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFValidateCaepSessionPresentedEvent extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		noteUntypedFieldIfNotString(eventData, "fp_ua");
		noteUntypedFieldIfNotString(eventData, "ext_id");

		logSuccess("Session Presented event fields are valid", args("event_data", eventData));

		return env;
	}

	/**
	 * Notes a present {@code fp_ua} or {@code ext_id} that is not a JSON string. The event
	 * definition assigns these fields no type, so a non-string value is recorded, not failed.
	 */
	private void noteUntypedFieldIfNotString(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null || OIDFJSON.isString(el)) {
			return;
		}
		log("Field '" + fieldName + "' is not a JSON string; the event definition assigns the field no type, so this is only noted",
			args(fieldName, el, "event_data", eventData));
	}
}
