package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * SSF 1.0 8.1.1 on {@code events_requested}: "This array SHOULD NOT be empty." Inspects the
 * parsed stream request body under {@code ssf.stream_input} (create, update and replace
 * requests alike) and raises a finding when the member is present but an empty array. An
 * absent member and a non-empty array pass; a member of the wrong type is graded by the
 * request validation condition, not here.
 */
public class OIDSSFWarnEmptyEventsRequestedInStreamRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement streamInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamInputEl == null || !streamInputEl.isJsonObject()) {
			log("No parsed stream request body available, nothing to inspect");
			return env;
		}

		JsonElement eventsRequestedEl = streamInputEl.getAsJsonObject().get("events_requested");
		if (eventsRequestedEl == null) {
			logSuccess("The stream request does not contain events_requested");
			return env;
		}

		if (eventsRequestedEl.isJsonArray() && eventsRequestedEl.getAsJsonArray().isEmpty()) {
			throw error("events_requested in the stream request is an empty array; a receiver should request at least one event type, as an empty array leaves the transmitter nothing to deliver",
				args("stream_request", streamInputEl));
		}

		logSuccess("events_requested in the stream request is not empty", args("events_requested", eventsRequestedEl));
		return env;
	}
}
