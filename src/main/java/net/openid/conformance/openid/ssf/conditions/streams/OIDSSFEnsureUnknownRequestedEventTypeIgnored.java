package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * SSF 1.0 8.1.1 on {@code events_requested}: "A Transmitter MUST ignore any array values that
 * it does not understand." The event type the suite made up
 * ({@code ssf.unknown_requested_event_type}, see
 * {@link OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent}) must therefore neither be
 * delivered nor claimed as supported in the stream configuration the transmitter returned
 * ({@code ssf.stream}).
 */
public class OIDSSFEnsureUnknownRequestedEventTypeIgnored extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String unknownEventType = env.getString("ssf", "unknown_requested_event_type");
		if (unknownEventType == null) {
			throw error("No unknown event type was added to the create request; nothing to check");
		}

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		if (streamEl == null || !streamEl.isJsonObject()) {
			throw error("Couldn't find the stream configuration returned by the transmitter", args("stream", streamEl));
		}

		if (contains(streamEl.getAsJsonObject().get("events_delivered"), unknownEventType)) {
			throw error("events_delivered contains the event type made up by the test suite. A transmitter MUST ignore "
					+ "requested event types it does not understand instead of promising to deliver them",
				args("unknown_event_type", unknownEventType, "events_delivered", streamEl.getAsJsonObject().get("events_delivered")));
		}
		if (contains(streamEl.getAsJsonObject().get("events_supported"), unknownEventType)) {
			throw error("events_supported contains the event type made up by the test suite; a transmitter cannot support "
					+ "an event type it does not understand",
				args("unknown_event_type", unknownEventType, "events_supported", streamEl.getAsJsonObject().get("events_supported")));
		}

		logSuccess("The transmitter ignored the unknown requested event type", args("unknown_event_type", unknownEventType));

		return env;
	}

	private static boolean contains(JsonElement arrayEl, String value) {
		return arrayEl != null && arrayEl.isJsonArray() && OIDFJSON.convertJsonArrayToList(arrayEl.getAsJsonArray()).contains(value);
	}
}
