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

		// Only the qualifying use cases of the published draft-01 (3.1-3.3) count here;
		// risk-level-change exists only in the WG head and does not qualify on its own.
		boolean foundAnyCaepInteropEvent = supportedEventTypes.stream().anyMatch(SsfEvents.CAEP_INTEROP_QUALIFYING_EVENT_TYPES::contains);
		if (!foundAnyCaepInteropEvent) {
			throw error("Could not find any CAEP Interop qualifying use case event in stream configuration",
				args("events_supported", supportedEventTypes, "expected_events", SsfEvents.CAEP_INTEROP_QUALIFYING_EVENT_TYPES));
		}

		logSuccess("Found at least one event described in the CAEP Interop qualifying use cases",
			args("events_supported", supportedEventTypes, "expected_events", SsfEvents.CAEP_INTEROP_QUALIFYING_EVENT_TYPES));

		return env;
	}
}
