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

	boolean hasEventsForStream(String streamId);

	/**
	 * Snapshot of the events currently queued for delivery (not yet handed to the
	 * receiver via push or poll), in queue order.
	 */
	List<OIDSSFSecurityEvent> getQueuedEvents(String streamId);

	/**
	 * Snapshot of every stored event for the stream that has not been acknowledged
	 * via {@link #registerAckForStreamEvent}. Note that push deliveries are not
	 * acknowledged through the event store, so for push streams this returns all
	 * stored events regardless of delivery outcome.
	 */
	List<OIDSSFSecurityEvent> getUnacknowledgedEvents(String streamId);

	void purgeStreamEvents(String streamId);

	OIDSSFSecurityEvent registerAckForStreamEvent(String streamId, String jti);

	OIDSSFSecurityEvent getRegisteredSecurityEvent(String streamId, String jti);

	boolean isStreamEventAcked(String streamId, String jti);

	void registerErrorForStreamEvent(String streamId, String jti, JsonObject error);

	JsonObject isErrorForStreamEvent(String streamId, String jti);

	void cleanup();
}
