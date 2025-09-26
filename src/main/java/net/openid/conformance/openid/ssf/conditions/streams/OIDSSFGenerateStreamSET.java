package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.function.BiConsumer;

public class OIDSSFGenerateStreamSET extends AbstractOIDSSFGenerateStreamSET {

	protected final String streamId;

	protected final JsonObject subject;

	protected final SsfEvent ssfEvent;

	protected BiConsumer<String, String> onStreamEventEnqueued;

	public OIDSSFGenerateStreamSET(OIDSSFEventStore eventStore, String streamId, JsonObject subject, SsfEvent ssfEvent, BiConsumer<String, String> onStreamEventEnqueued) {
		super(eventStore, ssfEvent.type());
		this.streamId = streamId;
		this.subject = subject;
		this.ssfEvent = ssfEvent;
		this.onStreamEventEnqueued = onStreamEventEnqueued;
	}

	@Override
	protected String getCurrentStreamId(Environment env) {
		return streamId;
	}

	@Override
	protected JsonObject getSubject(String streamId) {
		return subject;
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		return OIDFJSON.convertMapToJsonObject(ssfEvent.data());
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		onStreamEventEnqueued.accept(streamId, setJti);
	}
}
