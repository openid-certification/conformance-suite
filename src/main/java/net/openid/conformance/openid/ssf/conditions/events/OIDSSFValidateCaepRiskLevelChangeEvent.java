package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates the fields of a CAEP Risk Level Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.8">CAEP 1.0 Section 3.8</a>
 * (a qualifying use case per CAEP Interop Profile 3.4):
 * <ul>
 *   <li>{@code principal} (REQUIRED) - JSON string; USER, DEVICE, SESSION, TENANT, ORG_UNIT,
 *       GROUP "or any other entity", so unknown values are logged, not rejected</li>
 *   <li>{@code current_level} (REQUIRED) - LOW, MEDIUM, HIGH</li>
 *   <li>{@code previous_level} (OPTIONAL) - LOW, MEDIUM, HIGH; absence means the previous
 *       level is unknown to the transmitter</li>
 *   <li>{@code risk_reason} (RECOMMENDED) - JSON string when present</li>
 * </ul>
 * The {@code reason_admin} requirement of CAEP Interop Profile 3.4 is checked separately by
 * {@link OIDSSFEnsureCaepInteropEventReasonAdminPresent}.
 */
public class OIDSSFValidateCaepRiskLevelChangeEvent extends AbstractCondition {

	private static final Set<String> VALID_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

	private static final Set<String> KNOWN_PRINCIPALS = Set.of("USER", "DEVICE", "SESSION", "TENANT", "ORG_UNIT", "GROUP");

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		validatePrincipal(eventData);
		validateRiskLevel(eventData, "current_level", true);
		validateRiskLevel(eventData, "previous_level", false);
		validateRiskReason(eventData);

		logSuccess("Risk Level Change event fields are valid", args("event_data", eventData));

		return env;
	}

	private void validatePrincipal(JsonObject eventData) {
		JsonElement el = eventData.get("principal");
		if (el == null) {
			throw error("Missing required field 'principal' in risk-level-change event",
				args("event_data", eventData));
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString() || OIDFJSON.getString(el).isEmpty()) {
			throw error("Field 'principal' MUST be a non-empty JSON string",
				args("principal", el, "event_data", eventData));
		}
		String principal = OIDFJSON.getString(el);
		if (!KNOWN_PRINCIPALS.contains(principal)) {
			// CAEP 1.0 3.8 allows "any other entity" - note it for the reviewer only
			log("Field 'principal' uses a value outside the entities the event definition names; this is permitted, since any other entity may be named",
				args("principal", principal, "named_principals", KNOWN_PRINCIPALS));
		}
	}

	private void validateRiskLevel(JsonObject eventData, String fieldName, boolean required) {
		JsonElement el = eventData.get(fieldName);
		if (el == null) {
			if (required) {
				throw error("Missing required field '" + fieldName + "' in risk-level-change event",
					args("event_data", eventData));
			}
			// previous_level absent: the previous risk level is unknown to the transmitter
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field '" + fieldName + "' MUST be a JSON string",
				args(fieldName, el, "event_data", eventData));
		}
		String value = OIDFJSON.getString(el);
		if (!VALID_RISK_LEVELS.contains(value)) {
			throw error("Field '" + fieldName + "' MUST be one of: LOW, MEDIUM, HIGH",
				args(fieldName, value, "valid_values", VALID_RISK_LEVELS));
		}
	}

	private void validateRiskReason(JsonObject eventData) {
		JsonElement el = eventData.get("risk_reason");
		if (el == null) {
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Field 'risk_reason' MUST be a JSON string when present",
				args("risk_reason", el, "event_data", eventData));
		}
	}
}
