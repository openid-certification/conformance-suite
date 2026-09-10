package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * SSF 1.0 8.1.2.1, status {@code paused}: "The Transmitter MUST NOT transmit events over the
 * stream."
 * <p>
 * Checks that the poll response at {@code ssf_polling_response} returned by a paused stream
 * carries no SETs.
 */
public class OIDSSFEnsureNoSetsReturnedWhileStreamPaused extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonObject pollingResponse = env.getObject("ssf_polling_response");
		JsonElement bodyJsonEl = pollingResponse.get("body_json");
		if (bodyJsonEl == null || !bodyJsonEl.isJsonObject()) {
			throw error("The poll response does not contain a JSON object", args("polling_response", pollingResponse));
		}

		JsonElement setsEl = bodyJsonEl.getAsJsonObject().get("sets");
		if (setsEl != null && setsEl.isJsonObject() && !setsEl.getAsJsonObject().isEmpty()) {
			JsonObject sets = setsEl.getAsJsonObject();
			throw error("The transmitter returned SETs from a paused stream. "
					+ "No events may be transmitted while the stream status is 'paused'.",
				args("set_count", sets.size(), "set_jtis", sets.keySet(), "polling_response", bodyJsonEl));
		}

		logSuccess("The paused stream returned no SETs", args("polling_response", bodyJsonEl));

		return env;
	}
}
