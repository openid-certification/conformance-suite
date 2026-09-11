package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the recommendation for {@code risk_reason} in a CAEP Risk Level Change event
 * (CAEP 1.0 3.8.1: "risk_reason RECOMMENDED, JSON string"). Throws when the field is absent;
 * callers invoke this at WARNING severity since it is a RECOMMENDED field. Its type is checked
 * by {@link OIDSSFValidateCaepRiskLevelChangeEvent}. Reads the event payload from
 * {@code ssf.caep_event.data}.
 */
public class OIDSSFWarnCaepRiskLevelChangeRiskReasonMissing extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		if (!eventData.has("risk_reason")) {
			throw error("risk_reason is missing from the risk-level-change event; it is a recommended field "
					+ "that tells the receiver why the risk level changed",
				args("event_data", eventData));
		}

		logSuccess("risk_reason is present", args("risk_reason", eventData.get("risk_reason")));
		return env;
	}
}
