package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies that the receiver requested at least one of the CAEP Interop event types
 * 'session-revoked', 'credential-change', 'device-compliance-change' or 'risk-level-change' for the given stream.
 * The CAEP Interoperability Profile 1.0 Section 3 requires implementations to support
 * at least one of these use cases.
 */
public class OIDSSFEnsureStreamContainsCaepInteropEvent extends AbstractCondition {

	private final String streamId;

	public OIDSSFEnsureStreamContainsCaepInteropEvent(String streamId) {
		this.streamId = streamId;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			throw error("Could not find stream configuration", args("stream_id", streamId));
		}

		// SSF 1.0 8.1.1.1 makes events_requested optional; a receiver that omits it
		// accepts whatever the transmitter delivers, so fall back to events_delivered.
		JsonElement eventsRequestedEl = streamConfig.get("events_requested");
		String checkedField = "events_requested";
		JsonElement effectiveEventsEl = eventsRequestedEl;
		if (effectiveEventsEl == null || !effectiveEventsEl.isJsonArray()) {
			checkedField = "events_delivered";
			effectiveEventsEl = streamConfig.get("events_delivered");
		}

		List<String> effectiveEvents = effectiveEventsEl == null || !effectiveEventsEl.isJsonArray()
			? List.of()
			: OIDFJSON.convertJsonArrayToList(effectiveEventsEl.getAsJsonArray());

		Set<String> caepInteropEvents = new LinkedHashSet<>(effectiveEvents);
		caepInteropEvents.retainAll(SsfEvents.CAEP_INTEROP_EVENT_TYPES);

		if (caepInteropEvents.isEmpty()) {
			throw error("Stream must include at least one of the CAEP Interop event types 'session-revoked', 'credential-change', 'device-compliance-change', 'risk-level-change'",
				args("stream_id", streamId, "checked_field", checkedField, "events", effectiveEvents, "caep_interop_event_types", SsfEvents.CAEP_INTEROP_EVENT_TYPES));
		}

		logSuccess("Stream includes at least one CAEP Interop event type",
			args("stream_id", streamId, "checked_field", checkedField, "caep_interop_event_types_found", caepInteropEvents));

		return env;
	}
}
