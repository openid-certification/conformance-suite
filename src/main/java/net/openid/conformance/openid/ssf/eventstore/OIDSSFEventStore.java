package net.openid.conformance.openid.ssf.eventstore;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * Poor man's SSF EventStore
 */
public interface OIDSSFEventStore {

	void storeEvent(String streamId, JsonObject eventObject);

	PollInfo pollEvents(String streamId, int maxCount, boolean waitForEvents, long waitTimeSeconds);

	void purgeStreamEvents(String streamId);

	record PollInfo(List<JsonObject> events, boolean moreAvailable) {
	}

	void registerAckForStreamEvent(String streamId, String jti);

	boolean isStreamEventAck(String streamId, String jti);

	void registerErrorForStreamEvent(String streamId, String jti, JsonObject error);

	JsonObject isErrorForStreamEvent(String streamId, String jti);
}
