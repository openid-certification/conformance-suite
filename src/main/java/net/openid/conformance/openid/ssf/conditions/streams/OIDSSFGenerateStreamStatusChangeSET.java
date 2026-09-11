package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;

import java.util.function.BiConsumer;

/**
 * A stream-updated event announcing a status the transmitter is about to set, rather than the
 * status stored in the stream configuration. SSF 1.0 8.1.5: "If a Transmitter decides to change
 * the status of an Event Stream from enabled to either paused or disabled, then the Transmitter
 * MUST send this event to the Receiver before stopping the stream", and on re-enabling "it MUST
 * send this event to the Receiver upon re-enabling the stream". The announcement is generated
 * while the stream still transmits, so it can be delivered before the status takes effect.
 */
public class OIDSSFGenerateStreamStatusChangeSET extends OIDSSFGenerateStreamStatusUpdatedSET {

	protected final StreamStatusValue announcedStatus;

	protected final String reason;

	public OIDSSFGenerateStreamStatusChangeSET(OIDSSFEventStore eventStore, String streamId, StreamStatusValue announcedStatus, String reason,
		BiConsumer<String, String> onStreamEventEnqueued) {
		super(eventStore, streamId, onStreamEventEnqueued);
		this.announcedStatus = announcedStatus;
		this.reason = reason;
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("status", announcedStatus.name());
		if (reason != null) {
			eventData.addProperty("reason", reason);
		}
		return eventData;
	}
}
