package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * RFC 8936 2.2 defines {@code maxEvents} as "the maximum number of unacknowledged SETs to
 * be returned" and says the transmitter SHOULD NOT send more; with {@code maxEvents} 0 (an
 * acknowledge-only request, 2.4.2) the receiver has said it wants no SETs at all. A SHOULD,
 * so callers grade this as a WARNING. Reads the response from {@code ssf_polling_response}
 * and the request that was sent from {@code ssf.poll.request}; nothing is checked when the
 * request carried no {@code maxEvents} or the response has no well-formed {@code sets}
 * object (that is {@link OIDSSFValidatePollResponse}'s job).
 */
public class OIDSSFWarnPollResponseExceedsMaxEvents extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf_polling_response", "ssf"})
	public Environment evaluate(Environment env) {

		Integer maxEvents = env.getInteger("ssf", "poll.request.maxEvents");
		if (maxEvents == null) {
			log("The poll request carried no maxEvents; the transmitter chooses how many SETs to return");
			return env;
		}

		JsonElement setsEl = env.getElementFromObject("ssf_polling_response", "body_json.sets");
		if (setsEl == null || !setsEl.isJsonObject()) {
			log("The poll response has no 'sets' object; nothing to compare with maxEvents");
			return env;
		}
		JsonObject sets = setsEl.getAsJsonObject();

		if (sets.size() > maxEvents) {
			throw error("The poll response contains more SETs than the 'maxEvents' of the request asked for",
				args("maxEvents", maxEvents, "returned_sets", sets.size(), "jtis", sets.keySet()));
		}

		logSuccess("The poll response does not exceed the 'maxEvents' of the request",
			args("maxEvents", maxEvents, "returned_sets", sets.size()));
		return env;
	}
}
