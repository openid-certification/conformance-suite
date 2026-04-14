package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFExtractReceivedSETs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonObject ssfPollingResponse = env.getObject("ssf_polling_response");
		JsonObject bodyJson = ssfPollingResponse.getAsJsonObject("body_json");
		if (bodyJson == null) {
			throw error("Missing json body in polling response", args("polling_response", ssfPollingResponse));
		}

		JsonObject setsObject = bodyJson.getAsJsonObject("sets");
		if (setsObject != null && !setsObject.isEmpty()) {
			env.putObject("ssf", "poll.sets", setsObject);
			logSuccess("Extracted sets", args("sets", setsObject, "set_keys", setsObject.keySet()));
		} else {
			// Reset poll.sets to an empty object so the next POLL_AND_ACKNOWLEDGE iteration
			// does not re-ack jtis from the previous (non-empty) poll response. Per RFC 8936
			// §2.4, a transmitter that has acknowledged a SET removes it from its queue, so
			// the receiver should only ack what it most recently received.
			env.putObject("ssf", "poll.sets", new JsonObject());
			log("Found empty or missing sets in polling response; cleared ssf.poll.sets",
				args("polling_response", ssfPollingResponse));
		}

		return env;
	}
}
