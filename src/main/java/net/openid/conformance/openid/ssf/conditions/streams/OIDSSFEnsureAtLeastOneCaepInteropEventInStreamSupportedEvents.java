package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class OIDSSFEnsureAtLeastOneCaepInteropEventInStreamSupportedEvents extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement supportedEventTypesEl = env.getElementFromObject("ssf", "stream.events_supported");
		if (supportedEventTypesEl == null) {
			throw error("Could not find events_supported in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
		}

		List<String> supportedEventTypes = OIDFJSON.convertJsonArrayToList(supportedEventTypesEl.getAsJsonArray());

		boolean foundAnyCaepInteropEvent = supportedEventTypes.stream().anyMatch(SsfEvents.CAEP_INTEROP_EVENT_TYPES::contains);
		if (!foundAnyCaepInteropEvent) {
			throw error("Could not find any event from the CAEP Interop spec in stream configuration",
				args("events_supported", supportedEventTypes, "expected_events", SsfEvents.CAEP_INTEROP_EVENT_TYPES));
		}

		logSuccess("Found at least one event described in CAEP Interop use-cases",
			args("events_supported", supportedEventTypes, "expected_events", SsfEvents.CAEP_INTEROP_EVENT_TYPES));

		return env;
	}
}
