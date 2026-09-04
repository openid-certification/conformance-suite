package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * CAEP Interop Profile 3.1 / 3.2 / 3.3: for the interop use cases
 * (session-revoked, credential-change, device-compliance-change) the
 * {@code reason_admin} field "MUST be populated with a non-empty object".
 * <p>
 * The shape of the object (BCP47-tagged string values) is validated separately
 * by {@link OIDSSFValidateCaepCommonOptionalFields}; this condition only
 * asserts presence and non-emptiness, which plain CAEP 1.0 does not require.
 */
public class OIDSSFEnsureCaepInteropEventReasonAdminPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();
		String eventType = env.getString("ssf", "caep_event.type");

		JsonElement reasonAdminEl = eventData.get("reason_admin");
		if (reasonAdminEl == null) {
			throw error("reason_admin is missing. The CAEP Interop Profile (3.1/3.2/3.3) requires "
					+ "reason_admin to be populated with a non-empty object for this event type.",
				args("event_type", eventType, "event_data", eventData));
		}

		if (!reasonAdminEl.isJsonObject() || reasonAdminEl.getAsJsonObject().isEmpty()) {
			throw error("reason_admin must be a non-empty object (CAEP Interop Profile 3.1/3.2/3.3)",
				args("event_type", eventType, "reason_admin", reasonAdminEl));
		}

		logSuccess("reason_admin is present and non-empty", args("event_type", eventType, "reason_admin", reasonAdminEl));

		return env;
	}
}
