package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SSF 1.0 8.1.1 on {@code events_requested}: "A Transmitter MUST ignore any array values that
 * it does not understand". The emulated transmitter ignores them as required; this condition
 * notes them, since a value outside the advertised {@code events_supported} is worth a look
 * (a misspelled or stale event type, or one this transmitter simply does not offer) but is not
 * discouraged by the specification, so callers grade it at INFO. Inspects the parsed stream
 * request body under {@code ssf.stream_input} (create, update and replace requests alike)
 * against the advertised {@code ssf.default_config.events_supported}.
 */
public class OIDSSFLogUnknownEventsRequestedInStreamRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement eventsRequestedEl = env.getElementFromObject("ssf", "stream_input.events_requested");
		if (eventsRequestedEl == null || !eventsRequestedEl.isJsonArray()) {
			log("The stream request carries no events_requested array, nothing to compare with events_supported");
			return env;
		}
		JsonElement eventsSupportedEl = env.getElementFromObject("ssf", "default_config.events_supported");
		if (eventsSupportedEl == null || !eventsSupportedEl.isJsonArray()) {
			log("The emulated transmitter advertises no events_supported, nothing to compare with");
			return env;
		}

		List<String> eventsSupported = OIDFJSON.convertJsonArrayToList(eventsSupportedEl.getAsJsonArray());
		Set<String> unknown = new LinkedHashSet<>();
		for (JsonElement el : eventsRequestedEl.getAsJsonArray()) {
			String eventType = OIDFJSON.isString(el) ? OIDFJSON.getString(el) : null;
			if (eventType == null || !eventsSupported.contains(eventType)) {
				unknown.add(String.valueOf(el));
			}
		}
		if (!unknown.isEmpty()) {
			throw error("events_requested in the stream request contains event types the transmitter does not advertise in events_supported; "
					+ "they are ignored, as the specification requires. Check them for misspelled or stale event types",
				args("unknown_events_requested", unknown, "events_supported", eventsSupported));
		}

		logSuccess("Every event type in events_requested is advertised in events_supported", args("events_requested", eventsRequestedEl));
		return env;
	}
}
