package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureSecurityEventTokenContainsSingleEvent extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setTokenClaimsEl = env.getElementFromObject("set_token", "claims");

		if (setTokenClaimsEl == null) {
			throw error("Couldn't find SET token claims");
		}

		JsonObject setTokenClaims = setTokenClaimsEl.getAsJsonObject();

		JsonElement eventsEl = setTokenClaims.get("events");
		if (eventsEl == null || !eventsEl.isJsonObject()) {
			throw error("SET token does not contain an 'events' claim or it is not a JSON object",
				args("claims", setTokenClaims));
		}

		int eventCount = eventsEl.getAsJsonObject().size();
		if (eventCount != 1) {
			throw error("The 'events' claim of the SET MUST contain only one event",
				args("event_count", eventCount, "events", eventsEl));
		}

		logSuccess("The 'events' claim contains exactly one event", args("events", eventsEl));

		return env;
	}
}
