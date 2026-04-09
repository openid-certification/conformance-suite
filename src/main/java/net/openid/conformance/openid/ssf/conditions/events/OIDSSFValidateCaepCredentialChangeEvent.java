package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the required fields for a CAEP Credential Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.3">CAEP 1.0 Section 3.3</a>:
 * <ul>
 *   <li>{@code credential_type} (REQUIRED) - password, pin, x509, fido2-platform, fido2-roaming,
 *       fido-u2f, verifiable-credential, phone-voice, phone-sms, app</li>
 *   <li>{@code change_type} (REQUIRED) - create, revoke, update, delete</li>
 * </ul>
 */
public class OIDSSFValidateCaepCredentialChangeEvent extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateRequiredStringField(eventData, "credential_type");
		validateRequiredStringField(eventData, "change_type");

		logSuccess("Credential Change event fields are valid", args("event_data", eventData));

		return env;
	}

	private void validateRequiredStringField(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			throw error("Missing required field '" + fieldName + "' in credential-change event",
				args("event_data", eventData));
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field '" + fieldName + "' MUST be a JSON string",
				args(fieldName, el, "event_data", eventData));
		}
	}
}
