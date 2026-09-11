package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

/**
 * RFC 8417 2.2 on the {@code events} claim: "For each name present, the corresponding value
 * MUST be a JSON object. The JSON object MAY be an empty object". Reads the parsed SET at
 * {@code set_token}.
 */
public class OIDSSFEnsureSecurityEventValuesAreJsonObjects extends AbstractCondition {

	@Override
	@PreEnvironment(required = "set_token")
	public Environment evaluate(Environment env) {

		JsonElement eventsEl = env.getElementFromObject("set_token", "claims.events");
		if (eventsEl == null || !eventsEl.isJsonObject()) {
			log("The SET has no 'events' object; that is graded by the single-event check", args("claims", env.getElementFromObject("set_token", "claims")));
			return env;
		}

		JsonObject events = eventsEl.getAsJsonObject();
		for (Map.Entry<String, JsonElement> event : events.entrySet()) {
			if (!event.getValue().isJsonObject()) {
				throw error("The value of each member of the 'events' claim must be a JSON object, an empty one when the event carries no data",
					args("event_type", event.getKey(), "value", event.getValue()));
			}
		}

		logSuccess("Every event in the 'events' claim carries a JSON object", args("event_types", events.keySet()));
		return env;
	}
}
