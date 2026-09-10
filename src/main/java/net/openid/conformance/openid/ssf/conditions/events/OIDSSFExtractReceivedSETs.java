package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFExtractReceivedSETs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonObject ssfPollingResponse = env.getObject("ssf_polling_response");
		JsonObject bodyJson = ssfPollingResponse.getAsJsonObject("body_json");
		if (bodyJson == null) {
			throw error("Missing json body in polling response", args("polling_response", ssfPollingResponse));
		}

		// RFC 8936 2.3: moreAvailable MAY be omitted, meaning false. Loops read it to poll
		// again right away instead of waiting out their interval.
		JsonElement moreAvailableEl = bodyJson.get("moreAvailable");
		boolean moreAvailable = moreAvailableEl != null && moreAvailableEl.isJsonPrimitive()
			&& moreAvailableEl.getAsJsonPrimitive().isBoolean() && OIDFJSON.getBoolean(moreAvailableEl);
		env.putString("ssf", "poll.more_available", Boolean.toString(moreAvailable));

		JsonObject setsObject = bodyJson.getAsJsonObject("sets");
		if (setsObject != null && !setsObject.isEmpty()) {
			env.putObject("ssf", "poll.sets", setsObject);
			logSuccess("Extracted sets", args("sets", setsObject, "set_keys", setsObject.keySet(), "moreAvailable", moreAvailable));
		} else {
			// Reset poll.sets to an empty object so the next POLL_AND_ACKNOWLEDGE iteration
			// does not re-ack jtis from the previous (non-empty) poll response. Per RFC 8936
			// §2.4, a transmitter that has acknowledged a SET removes it from its queue, so
			// the receiver should only ack what it most recently received.
			env.putObject("ssf", "poll.sets", new JsonObject());
			log("Found empty or missing sets in polling response",
				args("polling_response", bodyJson));
		}

		return env;
	}
}
