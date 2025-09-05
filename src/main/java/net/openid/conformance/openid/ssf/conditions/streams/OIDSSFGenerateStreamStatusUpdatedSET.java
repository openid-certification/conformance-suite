package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class OIDSSFGenerateStreamStatusUpdatedSET extends AbstractOIDSSFGenerateStreamSET {

	public OIDSSFGenerateStreamStatusUpdatedSET(OIDSSFEventStore eventStore) {
		super(eventStore);
	}

	@Override
	protected void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder) {

		JsonObject streamStatus = OIDSSFStreamUtils.getStreamStatus(streamConfig);

		JsonObject events = OIDFJSON.convertMapToJsonObject(Map.of(SsfEvents.STREAM_UPDATED_EVENT, streamStatus));

		claimsBuilder
			.claim("sub_id", generateStreamSubject(streamId))
			.claim("events", events);
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);

		// clear verification state
		streamConfig.remove("_verification_state");

		eventStore.storeEvent(streamId, new OIDSSFSecurityEvent(setJti, setTokenString, SsfEvents.STREAM_UPDATED_EVENT));
	}
}
