package net.openid.conformance.openid.ssf.eventstore;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;

import java.util.List;

/**
 * Poor man's SSF EventStore
 */
public interface OIDSSFEventStore {

	record EventsBatch(List<OIDSSFSecurityEvent> events, boolean moreAvailable) {
	}

	void storeEvent(String streamId, OIDSSFSecurityEvent eventObject);

	default EventsBatch pollEvents(String streamId, int maxCount) {
		return pollEvents(streamId, maxCount, false, 0);
	}

	EventsBatch pollEvents(String streamId, int maxCount, boolean waitForEvents, long waitTimeSeconds);

	void purgeStreamEvents(String streamId);

	OIDSSFSecurityEvent registerAckForStreamEvent(String streamId, String jti);

	OIDSSFSecurityEvent getRegisteredSecurityEvent(String streamId, String jti);

	boolean isStreamEventAcked(String streamId, String jti);

	void registerErrorForStreamEvent(String streamId, String jti, JsonObject error);

	JsonObject isErrorForStreamEvent(String streamId, String jti);
}
