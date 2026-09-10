package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;

import java.util.function.BiConsumer;

/**
 * A stream-updated SET whose event object carries, next to {@code status} and {@code reason},
 * one member no specification defines ({@link SsfEvents#UNKNOWN_EVENT_MEMBER_NAME}). Transmitters
 * may add such members anywhere in a SET, and receivers have to ignore the ones they do not
 * understand (SSF 1.0 4.2.3), so the receiver under test must still accept the event.
 */
public class OIDSSFGenerateStreamStatusUpdatedSETWithUnknownMember extends OIDSSFGenerateStreamStatusUpdatedSET {

	public OIDSSFGenerateStreamStatusUpdatedSETWithUnknownMember(OIDSSFEventStore eventStore, String streamId, BiConsumer<String, String> onStreamEventEnqueued) {
		super(eventStore, streamId, onStreamEventEnqueued);
	}

	@Override
	protected void addAdditionalEventMembers(JsonObject eventData) {
		eventData.addProperty(SsfEvents.UNKNOWN_EVENT_MEMBER_NAME, SsfEvents.UNKNOWN_EVENT_MEMBER_VALUE);
	}
}
