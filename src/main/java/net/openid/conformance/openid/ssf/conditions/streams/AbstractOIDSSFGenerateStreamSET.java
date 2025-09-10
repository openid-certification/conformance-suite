package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public abstract class AbstractOIDSSFGenerateStreamSET extends AbstractOIDSSFGenerateSET {

	public AbstractOIDSSFGenerateStreamSET(OIDSSFEventStore eventStore, String eventType) {
		super(eventStore, eventType);
	}

	@Override
	protected String getCurrentStreamId(Environment env) {
		return env.getString("incoming_request", "body_json.stream_id");
	}

	protected JsonObject generateStreamSubject(String streamId) {
		JsonObject subject = new JsonObject();
		subject.addProperty("format", "opaque");
		subject.addProperty("id", streamId);
		return subject;
	}

	@Override
	protected void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder) {

		JsonObject eventData = getEventData(streamConfig);
		JsonObject events = OIDFJSON.convertMapToJsonObject(Map.of(eventType, eventData));

		claimsBuilder
			.claim("sub_id", getSubject(streamId))
			.claim("events", events);
	}

	protected JsonObject getSubject(String streamId) {
		return generateStreamSubject(streamId);
	}

	protected abstract JsonObject getEventData(JsonObject streamConfig);
}
