package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

import java.util.function.BiConsumer;

/**
 * Generates a stream-updated SET (SSF 1.0 8.1.5) reporting the stream's current status: the
 * event carries the REQUIRED {@code status} and, when the stream status has one, the OPTIONAL
 * {@code reason}; the top-level {@code sub_id} is the stream's opaque id. Subclasses may add
 * further members via {@link #addAdditionalEventMembers(JsonObject)}.
 */
public class OIDSSFGenerateStreamStatusUpdatedSET extends AbstractOIDSSFGenerateStreamSET {

	protected final String streamId;

	protected final BiConsumer<String, String> onStreamEventEnqueued;

	/** Generates the event for the stream named in the incoming request body. */
	public OIDSSFGenerateStreamStatusUpdatedSET(OIDSSFEventStore eventStore) {
		this(eventStore, null, (streamId, jti) -> {
		});
	}

	/**
	 * @param streamId             the stream to report on, or {@code null} to use the stream named in
	 *                             the incoming request body
	 * @param onStreamEventEnqueued notified with the stream id and the {@code jti} of the stored SET
	 */
	public OIDSSFGenerateStreamStatusUpdatedSET(OIDSSFEventStore eventStore, String streamId, BiConsumer<String, String> onStreamEventEnqueued) {
		super(eventStore, SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
		this.streamId = streamId;
		this.onStreamEventEnqueued = onStreamEventEnqueued;
	}

	@Override
	protected String getCurrentStreamId(Environment env) {
		return streamId != null ? streamId : super.getCurrentStreamId(env);
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		JsonObject streamStatus = OIDSSFStreamUtils.getStreamStatus(streamConfig);
		JsonObject eventData = new JsonObject();
		eventData.add("status", streamStatus.get("status"));
		JsonElement reason = streamStatus.get("reason");
		if (reason != null && !reason.isJsonNull()) {
			eventData.add("reason", reason);
		}
		addAdditionalEventMembers(eventData);
		return eventData;
	}

	/** Hook for members beyond {@code status} and {@code reason}; adds none by default. */
	protected void addAdditionalEventMembers(JsonObject eventData) {
		// NOOP
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		onStreamEventEnqueued.accept(streamId, setJti);
	}
}
