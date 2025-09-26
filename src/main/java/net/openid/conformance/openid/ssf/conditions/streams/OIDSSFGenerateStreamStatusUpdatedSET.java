package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;

public class OIDSSFGenerateStreamStatusUpdatedSET extends AbstractOIDSSFGenerateStreamSET {

	public OIDSSFGenerateStreamStatusUpdatedSET(OIDSSFEventStore eventStore) {
		super(eventStore, SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		return OIDSSFStreamUtils.getStreamStatus(streamConfig);
	}
}
