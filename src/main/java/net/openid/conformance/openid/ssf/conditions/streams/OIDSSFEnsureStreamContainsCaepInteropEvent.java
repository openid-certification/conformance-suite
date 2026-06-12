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
 * 'session-revoked', 'credential-change' or 'device-compliance-change' for the given stream.
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

		JsonElement eventsRequestedEl = streamConfig.get("events_requested");
		List<String> eventsRequested = eventsRequestedEl == null
			? List.of()
			: OIDFJSON.convertJsonArrayToList(eventsRequestedEl.getAsJsonArray());

		Set<String> requestedCaepInteropEvents = new LinkedHashSet<>(eventsRequested);
		requestedCaepInteropEvents.retainAll(SsfEvents.CAEP_INTEROP_EVENT_TYPES);

		if (requestedCaepInteropEvents.isEmpty()) {
			throw error("Stream must request at least one of the CAEP Interop event types 'session-revoked', 'credential-change', 'device-compliance-change'",
				args("stream_id", streamId, "events_requested", eventsRequested, "caep_interop_event_types", SsfEvents.CAEP_INTEROP_EVENT_TYPES));
		}

		logSuccess("Stream requests at least one CAEP Interop event type",
			args("stream_id", streamId, "requested_caep_interop_event_types", requestedCaepInteropEvents));

		return env;
	}
}
