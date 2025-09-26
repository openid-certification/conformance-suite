package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OIDSSFCheckSupportedEventsForStream extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement supportedEventTypesEl = env.getElementFromObject("ssf", "stream.events_supported");
		if (supportedEventTypesEl == null) {
			throw error("Could not find supported event types in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
		}

		List<String> supportedEventTypes = OIDFJSON.convertJsonArrayToList(supportedEventTypesEl.getAsJsonArray());

		Set<String> unknownEventTypes = findUnknownEventTypes(supportedEventTypes);

		if (!unknownEventTypes.isEmpty()) {
			throw error("Found unknown event types in stream configuration",
				args("unknown_events", unknownEventTypes, "events_supported", supportedEventTypes, "standard_event_types", SsfEvents.STANDARD_EVENT_TYPES));
		} else {
			logSuccess("Only supported event types found in stream configuration",
				args("events_supported", supportedEventTypes));
		}

		return env;
	}

	protected Set<String> findUnknownEventTypes(List<String> supportedEventTypes) {
		Set<String> unknownEventTypes = new LinkedHashSet<>(supportedEventTypes);
		unknownEventTypes.removeAll(SsfEvents.STANDARD_EVENT_TYPES);
		return unknownEventTypes;
	}
}
