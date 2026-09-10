package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds an event type no transmitter can know to {@code events_requested} of the prepared
 * stream configuration, so the create response can be checked against SSF 1.0 8.1.1:
 * "A Transmitter MUST ignore any array values that it does not understand." The type is kept
 * at {@code ssf.unknown_requested_event_type} for {@link OIDSSFEnsureUnknownRequestedEventTypeIgnored}.
 */
public class OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent extends AbstractOIDSSFPrepareStreamConfigObject {

	public static final String UNKNOWN_EVENT_TYPE = "https://schemas.openid.net/secevent/oidf-conformance-suite/event-type/unknown-event-type";

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = getStreamConfig(env);

		JsonElement eventsRequestedEl = streamConfig.get("events_requested");
		JsonArray eventsRequested = eventsRequestedEl != null && eventsRequestedEl.isJsonArray()
			? eventsRequestedEl.getAsJsonArray() : new JsonArray();
		eventsRequested.add(UNKNOWN_EVENT_TYPE);
		streamConfig.add("events_requested", eventsRequested);

		env.putString("ssf", "unknown_requested_event_type", UNKNOWN_EVENT_TYPE);

		log("Added an event type unknown to any transmitter to 'events_requested'; the transmitter must ignore it",
			args("unknown_event_type", UNKNOWN_EVENT_TYPE, "events_requested", eventsRequested));

		return env;
	}
}
