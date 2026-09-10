package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SSF 1.0 8.1.1: "events_delivered: ... This is a subset (not necessarily a proper subset) of
 * the intersection of events_supported and events_requested." Validates the transmitter's
 * arithmetic on the stream configuration it returned: a transmitter that ignores
 * events_requested, or claims to deliver event types it never advertised as supported, is
 * caught here. events_delivered and events_supported are read from the stream configuration
 * response; events_requested is taken from the request body the suite sent
 * ({@code ssf.expected_stream_config}), so a transmitter that rewrites the echoed
 * events_requested to match events_delivered does not pass. The echoed value is only used
 * when no sent request body is available.
 */
public class OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		if (streamEl == null || !streamEl.isJsonObject()) {
			throw error("Couldn't find stream configuration to validate", args("stream", streamEl));
		}

		JsonElement eventsDeliveredEl = streamEl.getAsJsonObject().get("events_delivered");
		if (eventsDeliveredEl == null || !eventsDeliveredEl.isJsonArray()) {
			// presence of events_delivered is checked elsewhere; nothing to compare here
			log("No events_delivered array in the stream configuration; skipping subset check",
				args("stream", streamEl));
			return env;
		}

		Set<String> eventsDelivered = new LinkedHashSet<>(OIDFJSON.convertJsonArrayToList(eventsDeliveredEl.getAsJsonArray()));

		JsonElement eventsSupportedEl = streamEl.getAsJsonObject().get("events_supported");
		JsonElement eventsRequestedEl = env.getElementFromObject("ssf", "expected_stream_config.events_requested");
		if (eventsRequestedEl == null) {
			log("No sent stream configuration available; comparing events_delivered against the events_requested echoed by the transmitter");
			eventsRequestedEl = streamEl.getAsJsonObject().get("events_requested");
		}

		Set<String> notCovered = new LinkedHashSet<>(eventsDelivered);
		if (eventsSupportedEl != null && eventsSupportedEl.isJsonArray()) {
			Set<String> eventsSupported = new LinkedHashSet<>(OIDFJSON.convertJsonArrayToList(eventsSupportedEl.getAsJsonArray()));
			notCovered.removeIf(eventsSupported::contains);
			if (!notCovered.isEmpty()) {
				throw error("events_delivered contains event types missing from events_supported. "
						+ "events_delivered is defined as a subset of the intersection of events_supported and events_requested.",
					args("events_delivered", eventsDelivered, "events_supported", eventsSupported, "not_supported", notCovered));
			}
		} else {
			log("No events_supported array in the stream configuration; cannot compare events_delivered against it",
				args("stream", streamEl));
		}

		if (eventsRequestedEl != null && eventsRequestedEl.isJsonArray()) {
			Set<String> eventsRequested = new LinkedHashSet<>(OIDFJSON.convertJsonArrayToList(eventsRequestedEl.getAsJsonArray()));
			Set<String> notRequested = new LinkedHashSet<>(eventsDelivered);
			notRequested.removeIf(eventsRequested::contains);
			if (!notRequested.isEmpty()) {
				throw error("events_delivered contains event types the receiver never requested. "
						+ "events_delivered is defined as a subset of the intersection of events_supported and events_requested.",
					args("events_delivered", eventsDelivered, "events_requested", eventsRequested, "not_requested", notRequested));
			}
		} else {
			log("No events_requested array in the stream configuration; cannot compare events_delivered against it",
				args("stream", streamEl));
		}

		logSuccess("events_delivered is a subset of the intersection of events_supported and events_requested",
			args("events_delivered", eventsDelivered));

		return env;
	}
}
