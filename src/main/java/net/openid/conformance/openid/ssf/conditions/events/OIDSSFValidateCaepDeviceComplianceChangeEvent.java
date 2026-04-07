package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates the required fields for a CAEP Device Compliance Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.5">CAEP 1.0 Section 3.5</a>:
 * <ul>
 *   <li>{@code current_status} (REQUIRED) - compliant, not-compliant</li>
 *   <li>{@code previous_status} (REQUIRED) - compliant, not-compliant</li>
 * </ul>
 */
public class OIDSSFValidateCaepDeviceComplianceChangeEvent extends AbstractCondition {

	private static final Set<String> VALID_COMPLIANCE_STATUSES = Set.of("compliant", "not-compliant");

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validateRequiredStatus(eventData, "current_status");
		validateRequiredStatus(eventData, "previous_status");

		logSuccess("Device Compliance Change event fields are valid", args("event_data", eventData));

		return env;
	}

	private void validateRequiredStatus(JsonObject eventData, String fieldName) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			throw error("Missing required field '" + fieldName + "' in device-compliance-change event",
				args("event_data", eventData));
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field '" + fieldName + "' MUST be a JSON string",
				args(fieldName, el, "event_data", eventData));
		}
		String value = OIDFJSON.getString(el);
		if (!VALID_COMPLIANCE_STATUSES.contains(value)) {
			throw error("Field '" + fieldName + "' MUST be one of: compliant, not-compliant",
				args(fieldName, value, "valid_values", VALID_COMPLIANCE_STATUSES));
		}
	}
}
